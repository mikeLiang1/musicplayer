package org.example.project.core.manager

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.example.project.core.model.Song
import java.util.UUID

// QueueSong removed - using Song directly with uniqueId as instance identifier

/**
 * Repeat mode for queue playback.
 */
enum class RepeatMode {
    OFF, ALL, ONE
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
    val repeatMode: RepeatMode = RepeatMode.OFF
)

/**
 * Resolved view of the queue for UI consumption.
 */
data class ResolvedQueue(
    val history: List<Song> = emptyList(),
    val current: Song? = null,
    val manualUpNext: List<Song> = emptyList(),
    val normalUpNext: List<Song> = emptyList()
)

/**
 * Manages dual-queue architecture with separate manual and normal queues.
 * Pure Kotlin with no platform dependencies.
 */
class QueueManager {

    private val _queueState = MutableStateFlow(QueueState())
    val queueState: StateFlow<QueueState> = _queueState.asStateFlow()

    private val _resolvedQueue = MutableStateFlow(ResolvedQueue())
    val resolvedQueue: StateFlow<ResolvedQueue> = _resolvedQueue.asStateFlow()

    init {
        // Update resolved queue whenever state changes
        _queueState.value = _queueState.value
        updateResolvedQueue()
    }

    // ── Queue Setup ──────────────────────────────────────────────────────────

    /**
     * Sets the base queue and starts playback from the specified index.
     */
    fun setBaseQueue(songs: List<Song>, startIndex: Int = 0) {
        // Songs already have uniqueId (from search or persistence)
        // We don't generate new ones here to preserve persistence IDs
        _queueState.update { state ->
            state.copy(
                baseQueue = songs,
                currentBaseIndex = startIndex.coerceIn(0, songs.lastIndex),
                manualQueue = emptyList(),
                isShuffled = false,
                preShuffleBaseQueue = null,
                preShuffleBaseIndex = null
            )
        }
        updateResolvedQueue()
    }

    // ── Playback Navigation ─────────────────────────────────────────────────

    /**
     * Advances to the next song.
     * If manual queue is not empty: plays manualQueue[0], removes it, currentBaseIndex stays.
     * If empty: currentBaseIndex++. Handles repeat at end.
     */
    fun playNext() {
        _queueState.update { state ->
            if (state.currentManualSong != null) {
                // Just finished a manual song — check if more manual songs remain
                if (state.manualQueue.isNotEmpty()) {
                    val nextManual = state.manualQueue.first()
                    state.copy(
                        manualQueue = state.manualQueue.drop(1),
                        currentManualSong = nextManual
                    )
                } else {
                    // No more manual songs — advance base queue
                    val newIndex = when (state.repeatMode) {
                        RepeatMode.ONE -> state.currentBaseIndex
                        RepeatMode.ALL -> (state.currentBaseIndex + 1) % state.baseQueue.size
                        RepeatMode.OFF -> (state.currentBaseIndex + 1).coerceAtMost(state.baseQueue.lastIndex)
                    }
                    state.copy(currentBaseIndex = newIndex, currentManualSong = null)
                }
            } else if (state.manualQueue.isNotEmpty()) {
                // Currently on a base queue song, manual songs are next
                val nextManual = state.manualQueue.first()
                state.copy(
                    manualQueue = state.manualQueue.drop(1),
                    currentManualSong = nextManual
                )
            } else {
                // No manual songs — advance base queue
                val newIndex = when (state.repeatMode) {
                    RepeatMode.ONE -> state.currentBaseIndex
                    RepeatMode.ALL -> (state.currentBaseIndex + 1) % state.baseQueue.size
                    RepeatMode.OFF -> if (state.currentBaseIndex < state.baseQueue.lastIndex)
                        state.currentBaseIndex + 1 else state.currentBaseIndex
                }
                state.copy(currentBaseIndex = newIndex, currentManualSong = null)
            }
        }
        updateResolvedQueue()
    }

    /**
     * Goes back to the previous song.
     * currentBaseIndex-- (manual queue stays pinned after new current)
     */
    fun playPrevious() {
        _queueState.update { state ->
            if (state.currentManualSong != null) {
                // If playing a manual song, go back to the base queue current song
                state.copy(currentManualSong = null)
            } else {
                val newIndex = when (state.repeatMode) {
                    RepeatMode.ONE -> state.currentBaseIndex
                    else -> (state.currentBaseIndex - 1).coerceAtLeast(0)
                }
                state.copy(currentBaseIndex = newIndex, currentManualSong = null)
            }
        }
        updateResolvedQueue()
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
        updateResolvedQueue()
    }

    /**
     * Selects a song from the manual queue to play now.
     * Removes manualQueue[0 until index], plays manualQueue[index], removes it.
     */
    fun selectManualSong(index: Int) {
        _queueState.update { state ->
            if (index !in state.manualQueue.indices) return@update state
            val selectedSong = state.manualQueue[index]
            state.copy(
                manualQueue = state.manualQueue.drop(index + 1),  // remove selected and all before it
                currentManualSong = selectedSong
            )
        }
        updateResolvedQueue()
    }

    /**
     * Selects a song from the normal queue to play now.
     * Sets currentBaseIndex to index. Skipped songs become history.
     * Manual queue stays pinned after new current.
     */
    fun selectNormalSong(index: Int) {
        _queueState.update { state ->
            val baseQueue = state.baseQueue
            if (index !in baseQueue.indices) return@update state

            state.copy(currentBaseIndex = index)
        }
        updateResolvedQueue()
    }

    /**
     * Selects a song from history to play again.
     * Sets currentBaseIndex back to index.
     */
    fun selectHistorySong(index: Int) {
        _queueState.update { state ->
            val baseQueue = state.baseQueue
            if (index !in 0..state.currentBaseIndex) return@update state

            state.copy(currentBaseIndex = index)
        }
        updateResolvedQueue()
    }

    /**
     * Removes a song from the manual queue.
     */
    fun removeManualSong(index: Int) {
        _queueState.update { state ->
            val manualQueue = state.manualQueue.toMutableList()
            if (index !in manualQueue.indices) return@update state

            manualQueue.removeAt(index)
            state.copy(manualQueue = manualQueue)
        }
        updateResolvedQueue()
    }

    /**
     * Removes a song from the normal queue (only after currentBaseIndex).
     * Adjusts index if needed.
     */
    fun removeNormalSong(index: Int) {
        _queueState.update { state ->
            val baseQueue = state.baseQueue.toMutableList()
            val currentIndex = state.currentBaseIndex

            // Can only remove songs after current
            if (index <= currentIndex || index !in baseQueue.indices) return@update state

            baseQueue.removeAt(index)

            // If we removed a song before the current index in the original list,
            // we need to adjust currentBaseIndex
            val newCurrentIndex = if (index < currentIndex) currentIndex - 1 else currentIndex

            state.copy(
                baseQueue = baseQueue,
                currentBaseIndex = newCurrentIndex
            )
        }
        updateResolvedQueue()
    }

    /**
     * Moves a song within or between manual and normal queues.
     * Only songs after current can be moved.
     * Moving normal→manual inserts into manualQueue.
     * Moving manual→normal inserts into baseQueue after current.
     */
    fun moveSong(fromQueue: String, fromIndex: Int, toQueue: String, toIndex: Int) {
        _queueState.update { state ->
            val baseQueue = state.baseQueue.toMutableList()
            val manualQueue = state.manualQueue.toMutableList()
            val currentIndex = state.currentBaseIndex

            when {
                fromQueue == "normal" && toQueue == "normal" -> {
                    // Move within normal queue (after current)
                    if (fromIndex <= currentIndex || toIndex <= currentIndex) return@update state
                    if (fromIndex !in baseQueue.indices || toIndex !in baseQueue.indices) return@update state

                    val song = baseQueue.removeAt(fromIndex)
                    val adjustedToIndex = if (toIndex > fromIndex) toIndex - 1 else toIndex
                    baseQueue.add(adjustedToIndex, song)
                }

                fromQueue == "manual" && toQueue == "manual" -> {
                    // Move within manual queue
                    if (fromIndex !in manualQueue.indices || toIndex !in manualQueue.indices) return@update state

                    val song = manualQueue.removeAt(fromIndex)
                    val adjustedToIndex = if (toIndex > fromIndex) toIndex - 1 else toIndex
                    manualQueue.add(adjustedToIndex, song)
                }

                fromQueue == "normal" && toQueue == "manual" -> {
                    // Move from normal to manual queue
                    if (fromIndex <= currentIndex) return@update state
                    if (fromIndex !in baseQueue.indices || toIndex !in manualQueue.indices) return@update state

                    val song = baseQueue.removeAt(fromIndex)
                    manualQueue.add(toIndex, song)
                }

                fromQueue == "manual" && toQueue == "normal" -> {
                    // Move from manual to normal queue (insert after current)
                    if (fromIndex !in manualQueue.indices) return@update state

                    val song = manualQueue.removeAt(fromIndex)
                    val insertIndex = currentIndex + 1 + toIndex
                    baseQueue.add(insertIndex, song)
                }
            }

            state.copy(
                baseQueue = baseQueue,
                manualQueue = manualQueue
            )
        }
        updateResolvedQueue()
    }

    // ── Shuffle & Repeat ────────────────────────────────────────────────────

    /**
     * Shuffles only baseQueue[currentBaseIndex+1..end]. Manual queue untouched.
     */
    fun shuffle() {
        _queueState.update { state ->
            val baseQueue = state.baseQueue
            if (baseQueue.isEmpty() || state.currentBaseIndex >= baseQueue.lastIndex) return@update state

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
        updateResolvedQueue()
    }

    /**
     * Restores baseQueue from snapshot. Current song stays current. Manual queue untouched.
     */
    fun unshuffle() {
        _queueState.update { state ->
            val preShuffleBaseQueue = state.preShuffleBaseQueue ?: return@update state
            val preShuffleBaseIndex = state.preShuffleBaseIndex ?: return@update state

            // Find current song in pre-shuffle queue
            val currentSong = state.baseQueue.getOrNull(state.currentBaseIndex) ?: return@update state
            val newIndex = preShuffleBaseQueue.indexOfFirst { it.uniqueId == currentSong.uniqueId }
            if (newIndex == -1) return@update state

            state.copy(
                baseQueue = preShuffleBaseQueue,
                currentBaseIndex = newIndex,
                isShuffled = false,
                preShuffleBaseQueue = null,
                preShuffleBaseIndex = null
            )
        }
        updateResolvedQueue()
    }

    /**
     * Cycles repeat mode: OFF → ALL → ONE → OFF
     */
    fun toggleRepeatMode() {
        _queueState.update { state ->
            val nextMode = when (state.repeatMode) {
                RepeatMode.OFF -> RepeatMode.ALL
                RepeatMode.ALL -> RepeatMode.ONE
                RepeatMode.ONE -> RepeatMode.OFF
            }
            state.copy(repeatMode = nextMode)
        }
        updateResolvedQueue()
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
            state.repeatMode == RepeatMode.ONE -> true
            state.repeatMode == RepeatMode.ALL -> state.baseQueue.isNotEmpty()
            else -> state.currentBaseIndex < state.baseQueue.lastIndex
        }
    }

    /**
     * Checks if there's a previous song available.
     */
    fun hasPrevious(): Boolean {
        val state = _queueState.value
        return when (state.repeatMode) {
            RepeatMode.ONE -> true
            else -> state.currentBaseIndex > 0
        }
    }

    // ── Internal Helpers ───────────────────────────────────────────────────

    private fun updateResolvedQueue() {
        val state = _queueState.value
        val baseQueue = state.baseQueue
        val currentIndex = state.currentBaseIndex

        val current = state.currentManualSong ?: baseQueue.getOrNull(currentIndex)
        val history = baseQueue.take(currentIndex)
        val manualUpNext = state.manualQueue
        val normalUpNext = baseQueue.drop(currentIndex + 1)

        _resolvedQueue.value = ResolvedQueue(
            history = history,
            current = current,
            manualUpNext = manualUpNext,
            normalUpNext = normalUpNext
        )
    }

    // ── Utility Methods ────────────────────────────────────────────────────

    /**
     * Gets the flat list of songs for playback (current + manualUpNext + normalUpNext).
     */
    fun getPlaybackQueue(): List<Song> {
        val resolved = _resolvedQueue.value
        return listOfNotNull(resolved.current) + resolved.manualUpNext + resolved.normalUpNext
    }

    /**
     * Gets the current song for playback.
     */
    fun getCurrentSong(): Song? {
        return _resolvedQueue.value.current
    }

    /**
     * Replaces queues while preserving shuffle/repeat state.
     */
    fun replaceQueuesPreservingState(baseQueue: List<Song>, manualQueue: List<Song>, currentBaseIndex: Int) {
        _queueState.update { state ->
            state.copy(
                baseQueue = baseQueue,
                manualQueue = manualQueue,
                currentBaseIndex = currentBaseIndex
                // isShuffled, preShuffleBaseQueue, repeatMode all preserved
            )
        }
        updateResolvedQueue()
    }

    /**
     * Restores state from persistence.
     */
    fun restoreState(state: QueueState) {
        _queueState.value = state
        updateResolvedQueue()
    }
}
