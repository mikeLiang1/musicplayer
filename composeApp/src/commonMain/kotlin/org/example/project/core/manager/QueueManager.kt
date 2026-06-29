package org.example.project.core.manager

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import org.example.project.core.model.Song
import java.util.UUID


/**
 * Repeat mode for queue playback.
 */
enum class PlaybackMode {
    OFF, REPEAT, Infinite
}

/**
 * Internal state of the queue manager.
 */
data class QueueState(
    val baseQueue: List<Song> = emptyList(),         // the normal playlist
    val manualQueue: List<Song> = emptyList(),       // user-added "play next" songs
    val currentBaseIndex: Int = 0,                        // index in baseQueue
    val currentManualSong: Song? = null,             // currently playing manual song, if any
    val isShuffled: Boolean = false,
    val preShuffleBaseQueue: List<Song>? = null,     // snapshot before shuffle
    val preShuffleBaseIndex: Int? = null,                 // index before shuffle
    val playbackMode: PlaybackMode = PlaybackMode.OFF,
    val autoPlay: Boolean = false,
    val contextId: String? = null,
    val seenIds: Set<String> = emptySet()
) {
    // Computed properties for UI consumption (formerly in ResolvedQueue)
    val history: List<Song>
        get() {
            // If playing manual, the interrupted base song counts as "history"
            // so the manual song sits at index history.size
            return if (currentManualSong != null) {
                baseQueue.take(currentBaseIndex + 1)
            } else {
                baseQueue.take(currentBaseIndex)
            }
        }


    val current: Song?
        get() = currentManualSong ?: baseQueue.getOrNull(currentBaseIndex)

    val manualUpNext: List<Song>
        get() = manualQueue

    val normalUpNext: List<Song>
        get() = baseQueue.drop(currentBaseIndex + 1)

    // Flat playback queue (includes history for full MediaController queue)
    val playbackQueue: List<Song>
        get() = history + listOfNotNull(current) + manualUpNext + normalUpNext

    // Current index in the full playback queue (including history)
    val playbackCurrentIndex: Int
        get() = history.size
}

/**
 * Manages dual-queue architecture with separate manual and normal queues.
 * Pure Kotlin with no platform dependencies.
 */
class QueueManager() {

    private val _queueState = MutableStateFlow(QueueState())
    val queueState: StateFlow<QueueState> = _queueState.asStateFlow()

    val _intent = Channel<QueueIntent>(Channel.UNLIMITED)
    val intent: Flow<QueueIntent> = _intent.receiveAsFlow()

    // ── Queue Setup ──────────────────────────────────────────────────────────

    /**
     * Sets the base queue and starts playback from the specified index.
     */
    fun setBaseQueue(songs: List<Song>, contextId: String? = null, currentBaseIndex: Int = 0) {
        _queueState.update { state ->
            state.copy(
                baseQueue = songs,
                currentBaseIndex = currentBaseIndex,
                manualQueue = emptyList(),
                isShuffled = false,
                preShuffleBaseQueue = null,
                preShuffleBaseIndex = null,
                autoPlay = true,
                currentManualSong = null,
                contextId = contextId,
                seenIds = songs.map { it.uniqueId }.toSet()
            )
        }
        _intent.trySend(QueueIntent.NewQueue())
    }


    fun appendRadioSongs(songs: List<Song>) {
        _queueState.update { state ->
            val newSongs = songs.filter { it.uniqueId !in state.seenIds }
            state.copy(
                baseQueue = state.baseQueue + newSongs,
                // Keep the un-shuffled snapshot in sync so these songs survive unshuffle.
                preShuffleBaseQueue = state.preShuffleBaseQueue?.plus(newSongs),
                seenIds = state.seenIds + newSongs.map { it.uniqueId },
            )
        }
        _intent.trySend(QueueIntent.ReplaceQueue(_queueState.value.playbackCurrentIndex))
    }


    // ── Playback Navigation ─────────────────────────────────────────────────

    /**
     * Advances to the next song.
     * If manual queue is not empty: plays manualQueue[0], removes it, currentBaseIndex stays.
     * If empty: currentBaseIndex++. Handles repeat at end.
     */
    fun playNext(fromAutoAdvanced: Boolean = false) {

        var hasStructureChanged = false

        _queueState.update { state ->
            // If current song is manual, need to remove it from manual queue and tell exoplayer to rebuild
            if (state.currentManualSong != null) {
                hasStructureChanged = true
                if (state.manualQueue.isNotEmpty()) {
                    // Play next manual
                    state.copy(
                        manualQueue = state.manualQueue.drop(1),
                        currentManualSong = state.manualQueue.first()
                    )
                } else {
                    // Advance base
                    val newIndex = (state.currentBaseIndex + 1).coerceAtMost(state.baseQueue.lastIndex)
                    state.copy(currentBaseIndex = newIndex, currentManualSong = null)
                }
                // We are moving into a manual song from normal song
            } else if (state.manualQueue.isNotEmpty()) {
                state.copy(
                    manualQueue = state.manualQueue.drop(1),
                    currentManualSong = state.manualQueue.first()
                )
            } else {
                val newIndex = if (state.currentBaseIndex < state.baseQueue.lastIndex)
                    state.currentBaseIndex + 1 else state.currentBaseIndex
                state.copy(currentBaseIndex = newIndex, currentManualSong = null)
            }
        }

        val pci = _queueState.value.playbackCurrentIndex

        if (hasStructureChanged) {
            _intent.trySend(QueueIntent.SeekAndRebuild(mediaIndex = pci + 1, queueIndex = pci))
        } else if (!fromAutoAdvanced) {
            _intent.trySend(QueueIntent.SeekToItem(pci))
        }
    }

    /**
     * Goes back to the previous song.
     * currentBaseIndex-- (manual queue stays pinned after new current)
     */
    fun playPrevious() {
        val hadManualSong = _queueState.value.currentManualSong != null
        val offset = if (hadManualSong) 0 else 1
        val newIndex = (_queueState.value.currentBaseIndex - offset).coerceAtLeast(0)
        selectHistorySong(newIndex)
    }

    // ── Queue Manipulation ──────────────────────────────────────────────────

    /**
     * Adds a song to the manual queue.
     */
    fun addToManualQueue(song: Song) {
        // Generate new uniqueId when adding to manual queue
        val queueSong = song.copy(uniqueId = UUID.randomUUID().toString())
        _queueState.update { state ->
            state.copy(manualQueue = state.manualQueue + queueSong)
        }
        _intent.trySend(QueueIntent.ReplaceQueue(_queueState.value.playbackCurrentIndex))
    }

    /**
     * Plays song from queue.
     */
    fun playSongFromQueue(uniqueId: String) {
        val state = _queueState.value

        // 1. Already playing?
        if (uniqueId == state.current?.uniqueId) return

        // 2. Check History (Indices 0 to currentBaseIndex)
        // In your QueueState, history is baseQueue.take(currentBaseIndex)
        val historyIndex = state.baseQueue.take(state.currentBaseIndex).indexOfFirst { it.uniqueId == uniqueId }
        if (historyIndex != -1) {
            selectHistorySong(historyIndex)
            return
        }

        // 3. Check Manual Queue
        val manualIndex = state.manualQueue.indexOfFirst { it.uniqueId == uniqueId }
        if (manualIndex != -1) {
            selectManualSong(manualIndex)
            return
        }

        // 4. Check Normal Up Next (Indices currentBaseIndex + 1 to end)
        val normalIndex = state.baseQueue.indexOfFirst { it.uniqueId == uniqueId }
        if (normalIndex != -1) {
            // We found it in baseQueue. Since we already checked history,
            // this index is guaranteed to be in the "upcoming" part.
            selectNormalSong(normalIndex)
            return
        }
    }

    /**
     * Selects a song from the manual queue to play now.
     * Removes manualQueue[0 until index], plays manualQueue[index], removes it.
     */
    private fun selectManualSong(index: Int) {
        val hadManualSong = _queueState.value.currentManualSong != null
        _queueState.update { state ->
            if (index !in state.manualQueue.indices) return@update state
            val selectedSong = state.manualQueue[index]
            state.copy(
                manualQueue = state.manualQueue.drop(index + 1),  // remove selected and all before it
                currentManualSong = selectedSong
            )
        }
        val pci = _queueState.value.playbackCurrentIndex
        val removedBefore = index + if (hadManualSong) 1 else 0
        _intent.trySend(QueueIntent.SeekAndRebuild(mediaIndex = pci + removedBefore, pci))
    }

    /**
     * Selects a song from the normal queue to play now.
     * Sets currentBaseIndex to index. Skipped songs become history.
     * Manual queue stays pinned after new current.
     */
    private fun selectNormalSong(index: Int) {
        val hadManualSong = _queueState.value.currentManualSong != null
        val manualQueueSize = _queueState.value.manualQueue.size
        _queueState.update { state ->
            val baseQueue = state.baseQueue
            if (index !in baseQueue.indices) return@update state

            state.copy(currentBaseIndex = index, currentManualSong = null)
        }
        val offset = (if (hadManualSong) 1 else 0) + manualQueueSize
        if (offset > 0) {
            _intent.trySend(QueueIntent.SeekAndRebuild(mediaIndex = index + offset, index))
        } else {
            _intent.trySend(QueueIntent.SeekToItem(index))
        }
    }

    /**
     * Selects a song from history to play again.
     * Sets currentBaseIndex back to index.
     */
    private fun selectHistorySong(index: Int) {
        val hadManualSong = _queueState.value.currentManualSong != null
        val hasManualQueue = _queueState.value.manualQueue.isNotEmpty()
        _queueState.update { state ->
            if (index !in 0..state.currentBaseIndex) return@update state
            state.copy(currentBaseIndex = index, currentManualSong = null)
        }

        val pci = _queueState.value.playbackCurrentIndex
        if (hadManualSong || hasManualQueue) {
            _intent.trySend(QueueIntent.SeekAndRebuild(pci, pci))
        } else {
            _intent.trySend(QueueIntent.SeekToItem(pci))
        }
    }

    fun removeSong(uniqueId: String) {
        _queueState.update { state ->
            // 1. Check Manual Queue
            val manualIndex = state.manualQueue.indexOfFirst { it.uniqueId == uniqueId }
            if (manualIndex != -1) {
                val newManual = state.manualQueue.filterIndexed { index, _ -> index != manualIndex }
                return@update state.copy(manualQueue = newManual)
            }

            // 2. Check Base Queue (Upcoming only)
            val baseIndex = state.baseQueue.indexOfFirst { it.uniqueId == uniqueId }
            // Only allow removal if it's in the future (index > currentBaseIndex)
            if (baseIndex > state.currentBaseIndex) {
                val newBase = state.baseQueue.filterIndexed { index, _ -> index != baseIndex }
                // Remove from the snapshot too so it doesn't reappear on unshuffle.
                val newPreShuffle = state.preShuffleBaseQueue?.filter { it.uniqueId != uniqueId }
                return@update state.copy(baseQueue = newBase, preShuffleBaseQueue = newPreShuffle)
            }

            // 3. If not found or is currently playing/history, do nothing
            state
        }

        // 4. Notify Media Session that the list changed
        _intent.trySend(QueueIntent.ReplaceQueue(_queueState.value.playbackCurrentIndex))
    }

    // ── Shuffle & Repeat ────────────────────────────────────────────────────

    /**
     * Shuffles only baseQueue[currentBaseIndex+1..end]. Manual queue untouched.
     */
    fun shuffle() {
        _queueState.update { state ->
            val baseQueue = state.baseQueue
            // Nothing to do for an empty queue or if we're already shuffled
            // (re-shuffling would overwrite the true original snapshot).
            if (baseQueue.isEmpty() || state.isShuffled) return@update state

            val beforeCurrent = baseQueue.take(state.currentBaseIndex + 1)
            val afterCurrent = baseQueue.drop(state.currentBaseIndex + 1)
            val shuffledAfterCurrent = afterCurrent.shuffled()

            state.copy(
                baseQueue = beforeCurrent + shuffledAfterCurrent,
                isShuffled = true,
                preShuffleBaseQueue = baseQueue,
                preShuffleBaseIndex = state.currentBaseIndex
            )
        }
        _intent.trySend(QueueIntent.ReplaceQueue(_queueState.value.playbackCurrentIndex))
    }

    /**
     * Restores baseQueue from snapshot. Current song stays current. Manual queue untouched.
     */
    fun unshuffle() {
        _queueState.update { state ->
            val preShuffleBaseQueue = state.preShuffleBaseQueue ?: return@update state

            // Find current song in pre-shuffle queue
            val currentSong = state.baseQueue.getOrNull(state.currentBaseIndex) ?: return@update state
            val newIndex = preShuffleBaseQueue.indexOfFirst { it.uniqueId == currentSong.uniqueId }
            // Should never happen now that the snapshot is kept in sync, but if the current
            // song somehow isn't in it, clear shuffle on the live queue instead of wedging.
            if (newIndex == -1) {
                return@update state.copy(
                    isShuffled = false,
                    preShuffleBaseQueue = null,
                    preShuffleBaseIndex = null
                )
            }

            state.copy(
                baseQueue = preShuffleBaseQueue,
                currentBaseIndex = newIndex,
                isShuffled = false,
                preShuffleBaseQueue = null,
                preShuffleBaseIndex = null
            )
        }
        _intent.trySend(QueueIntent.ReplaceQueue(_queueState.value.playbackCurrentIndex))
    }

    /**
     * Cycles repeat mode: OFF → ALL → ONE → OFF
     */
    fun togglePlaybackMode() {
        _queueState.update { state ->
            val nextMode = when (state.playbackMode) {
                PlaybackMode.OFF -> PlaybackMode.REPEAT
                PlaybackMode.REPEAT -> PlaybackMode.Infinite
                PlaybackMode.Infinite -> PlaybackMode.OFF
            }
            state.copy(playbackMode = nextMode)
        }
    }
    // ── Query Methods ───────────────────────────────────────────────────────

    /**
     * Checks if there's a next song available (manual + normal + repeat mode).
     */
    fun hasNext(): Boolean {
        val state = _queueState.value
        return when {
            state.manualQueue.isNotEmpty() -> true
            state.currentManualSong != null -> true  // can go back to base queue
            state.playbackMode == PlaybackMode.Infinite -> true
            state.playbackMode == PlaybackMode.REPEAT -> state.baseQueue.isNotEmpty()
            else -> state.currentBaseIndex < state.baseQueue.lastIndex
        }
    }

    /**
     * Checks if there's a previous song available.
     */
    fun hasPrevious(): Boolean {
        val state = _queueState.value
        return when (state.playbackMode) {
            PlaybackMode.Infinite -> true
            else -> state.currentBaseIndex > 0
        }
    }


    /**
     * Replaces queues while preserving shuffle/repeat state.
     */
    fun replaceQueuesPreservingState(baseQueue: List<Song>, manualQueue: List<Song>, currentBaseIndex: Int) {
        _queueState.update { state ->
            // Reconcile the snapshot to the same set of songs as the new baseQueue:
            // keep originals still present (in original order), then append any that moved in.
            val newPreShuffle = state.preShuffleBaseQueue?.let { pre ->
                val baseIds = baseQueue.map { it.uniqueId }.toSet()
                val preIds = pre.map { it.uniqueId }.toSet()
                pre.filter { it.uniqueId in baseIds } + baseQueue.filter { it.uniqueId !in preIds }
            }
            state.copy(
                baseQueue = baseQueue,
                manualQueue = manualQueue,
                currentBaseIndex = currentBaseIndex,
                preShuffleBaseQueue = newPreShuffle
            )
        }
        _intent.trySend(QueueIntent.ReplaceQueue(_queueState.value.playbackCurrentIndex))
    }

    /**
     * Restores queue state from persistence.
     * Should be called during app initialization.
     */
    fun restoreState(state: QueueState, positionMs: Long) {
        _queueState.value = state
        _intent.trySend(QueueIntent.NewQueue(positionMs))
    }

}
