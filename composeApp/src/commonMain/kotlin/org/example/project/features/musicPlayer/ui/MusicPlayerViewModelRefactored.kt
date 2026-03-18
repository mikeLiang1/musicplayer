package org.example.project.features.musicPlayer.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.example.project.core.manager.MusicPlayerManager
import org.example.project.core.manager.QueueManager
import org.example.project.core.model.Song
import org.example.project.core.repository.YouTubeRepository
import org.example.project.features.musicPlayer.model.PlayerQueue

class MusicPlayerViewModelRefactored constructor(
    private val repository: YouTubeRepository,
    private val musicPlayerManager: MusicPlayerManager,
    private val queueManager: QueueManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(MusicPlayerUiState())
    val uiState = _uiState.asStateFlow()

    private val _effect = MutableSharedFlow<MusicPlayerEffect>()
    val effect: SharedFlow<MusicPlayerEffect> = _effect.asSharedFlow()

    val playerState = musicPlayerManager.playerState
    val currentPosition = musicPlayerManager.currentPosition

    // Projection from QueueManager's resolved queue
    val playerQueue: StateFlow<PlayerQueue> = queueManager.resolvedQueue
        .map { resolvedQueue ->
            PlayerQueue(
                history = resolvedQueue.history,
                current = resolvedQueue.current,
                manual = resolvedQueue.manualUpNext,
                upcoming = resolvedQueue.normalUpNext
            )
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, PlayerQueue())

    // What the UI consumes — editing snapshot if active, else live projection
    val displayQueue: StateFlow<PlayerQueue> = combine(_uiState, playerQueue) { ui, live ->
        ui.editingQueue ?: live
    }.stateIn(viewModelScope, SharingStarted.Eagerly, playerQueue.value)

    init {

        // Sync queue changes to music player
        queueManager.resolvedQueue
            .onEach { resolvedQueue ->
                val playbackQueue = queueManager.getPlaybackQueue()
                val currentSong = queueManager.getCurrentSong()
                val startIndex = playbackQueue.indexOfFirst { it.uniqueId == currentSong?.uniqueId }.coerceAtLeast(0)

                // Only update if there are songs to play
                if (playbackQueue.isNotEmpty()) {
//                    musicPlayerManager.setPlaylist(playbackQueue, startIndex, 0L)
                    musicPlayerManager.replaceFullQueueKeepingCurrentSong(songs = playbackQueue, startIndex)
                }
            }
            .launchIn(viewModelScope)
    }

    // ── Playback ──────────────────────────────────────
    fun onPlayPauseClicked() {
        if (playerState.value.isPlaying) musicPlayerManager.pause()
        else musicPlayerManager.play()
    }

    fun onNextClicked() = queueManager.playNext()
    fun onPreviousClicked() = queueManager.playPrevious()
    fun onSeekTo(seconds: Long) = musicPlayerManager.seekTo(seconds * 1000)

    fun changeShuffleOption() {
        val state = queueManager.queueState.value
        if (state.isShuffled) {
            queueManager.unshuffle()
        } else {
            queueManager.shuffle()
        }
    }

    fun toggleRepeatMode() = queueManager.toggleRepeatMode()

    // ── UI State ──────────────────────────────────────
    fun setFullScreen(fullScreen: Boolean) {
        _uiState.update { it.copy(isFullScreenVisible = fullScreen) }
    }

    fun onHistoryPillClicked() {
        viewModelScope.launch {
            if (uiState.value.showHistory) {
                _uiState.update { it.copy(showHistory = false) }
                _effect.emit(MusicPlayerEffect.ScrollToFirst)
            } else {
                _uiState.update { it.copy(showHistory = true) }
                _effect.emit(MusicPlayerEffect.ScrollUpWhenHistoryOpened)
            }
        }
    }

    fun changePlayingToSong(song: Song) {
        val resolvedQueue = queueManager.resolvedQueue.value

        // Check if song is in history
        val historyIndex = resolvedQueue.history.indexOfFirst { it.uniqueId == song.uniqueId }
        if (historyIndex != -1) {
            queueManager.selectHistorySong(historyIndex)
            return
        }

        // Check if song is in manual queue
        val manualIndex = resolvedQueue.manualUpNext.indexOfFirst { it.uniqueId == song.uniqueId }
        if (manualIndex != -1) {
            queueManager.selectManualSong(manualIndex)
            return
        }

        // Check if song is in normal upcoming queue
        val normalIndex = resolvedQueue.normalUpNext.indexOfFirst { it.uniqueId == song.uniqueId }
        if (normalIndex != -1) {
            // Convert to absolute index in base queue
            val currentIndex = queueManager.queueState.value.currentBaseIndex
            queueManager.selectNormalSong(currentIndex + 1 + normalIndex)
            return
        }

        // Song not found in queue
    }

    fun onMenuClicked(song: Song) {
        queueManager.addToManualQueue(song)
    }

    // ── Queue Edit ────────────────────────────────────
    fun onEditQueueClicked() {
        _uiState.update {
            it.copy(
                editingQueue = if (it.editingQueue != null) null else playerQueue.value,
                isEditingQueue = !_uiState.value.isEditingQueue
            )
        }
    }

    fun onMove(fromKey: String, toKey: String) {
        val current = _uiState.value.editingQueue ?: return

        // Combine manual and upcoming songs for drag-and-drop
        val future = (current.manual + current.upcoming).toMutableList()

        val fromIndex = future.indexOfFirst { it.uniqueId == fromKey }.takeIf { it != -1 } ?: return
        val toIndex = future.indexOfFirst { it.uniqueId == toKey }.takeIf { it != -1 } ?: return

        val item = future.removeAt(fromIndex)

        // Determine if we're moving between queues
        val wasManual = item.isManual
        val willBeManual = toIndex < current.manual.size

        val updatedItem = if (wasManual != willBeManual) {
            // Moving between queues - update isManual flag
            item.copy(isManual = willBeManual)
        } else {
            // Moving within same queue
            item
        }

        future.add(toIndex, updatedItem)

        // Recount from the final list state
        val newManualCount = future.count { it.isManual }

        _uiState.update {
            it.copy(
                editingQueue = current.copy(
                    manual = future.take(newManualCount),
                    upcoming = future.drop(newManualCount)
                )
            )
        }
    }

    fun onDragEnd() {
        val editing = _uiState.value.editingQueue ?: return

        // Apply the edited queue back to QueueManager
        viewModelScope.launch {
            // Get current queue state
            val state = queueManager.queueState.value
            val currentBaseIndex = state.currentBaseIndex

            // We need to reconstruct the dual-queue structure from the edited flat queue
            // The edited queue contains: history + current + manual + upcoming

            // For now, we'll take a simplified approach:
            // 1. Keep the current song as is
            // 2. Rebuild base queue from history + current + upcoming (non-manual songs)
            // 3. Rebuild manual queue from manual songs

            // Extract songs from edited queue
            val historySongs = editing.history
            val currentSong = editing.current
            val manualSongs = editing.manual
            val upcomingSongs = editing.upcoming

            // Reconstruct base queue (history + current + upcoming non-manual songs)
            val baseQueueSongs = historySongs + listOfNotNull(currentSong) + upcomingSongs

            // Set the new base queue
            queueManager.setBaseQueue(baseQueueSongs, historySongs.size)

            // Add manual songs back to manual queue
            manualSongs.forEach { song ->
                queueManager.addToManualQueue(song)
            }

            // Exit editing mode
            _uiState.update {
                it.copy(
                    editingQueue = null,
                    isEditingQueue = false
                )
            }
        }
    }

    // ── Queue Management ──────────────────────────────

    fun removeSong(song: Song) {
        val resolvedQueue = queueManager.resolvedQueue.value

        // Check if song is in manual queue
        val manualIndex = resolvedQueue.manualUpNext.indexOfFirst { it.uniqueId == song.uniqueId }
        if (manualIndex != -1) {
            queueManager.removeManualSong(manualIndex)
            return
        }

        // Check if song is in normal upcoming queue
        val normalIndex = resolvedQueue.normalUpNext.indexOfFirst { it.uniqueId == song.uniqueId }
        if (normalIndex != -1) {
            val currentIndex = queueManager.queueState.value.currentBaseIndex
            queueManager.removeNormalSong(currentIndex + 1 + normalIndex)
            return
        }

        // Song not found or is in history/current (can't remove those)
    }

    fun setQueue(songs: List<Song>, startIndex: Int = 0) {
        queueManager.setBaseQueue(songs, startIndex)
    }

    // ── App Lifecycle ────────────────────────────────────
}

data class MusicPlayerUiState(
    val isFullScreenVisible: Boolean = false,
    val showHistory: Boolean = false,
    val isEditingQueue: Boolean = false,
    val editingQueue: PlayerQueue? = null
)

sealed interface MusicPlayerEffect {
    data object ScrollUpWhenHistoryOpened : MusicPlayerEffect
    data object ScrollToFirst : MusicPlayerEffect
}
