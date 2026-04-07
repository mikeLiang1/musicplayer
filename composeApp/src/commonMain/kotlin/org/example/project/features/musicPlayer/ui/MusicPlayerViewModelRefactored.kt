package org.example.project.features.musicPlayer.ui

import android.util.Log
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
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.example.project.core.manager.MusicPlayerManager
import org.example.project.core.manager.PlaybackMode
import org.example.project.core.manager.QueueIntent
import org.example.project.core.manager.QueueManager
import org.example.project.core.model.Song
import org.example.project.core.repository.SavedDataRepository
import org.example.project.core.repository.YouTubeRepository
import org.example.project.features.musicPlayer.model.PlayerQueue

class MusicPlayerViewModelRefactored constructor(
    private val repository: YouTubeRepository,
    private val musicPlayerManager: MusicPlayerManager,
    private val queueManager: QueueManager,
    private val savedDataRepository: SavedDataRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MusicPlayerUiState())
    val uiState = _uiState.asStateFlow()

    private val _effect = MutableSharedFlow<MusicPlayerEffect>()
    val effect: SharedFlow<MusicPlayerEffect> = _effect.asSharedFlow()

    val playerState = musicPlayerManager.playerState
    val currentPosition = musicPlayerManager.currentPosition

    // Projection from QueueManager's state
    val playerQueue: StateFlow<PlayerQueue> = queueManager.queueState
        .map { state ->
            PlayerQueue(
                history = state.history,
                current = state.current,
                manual = state.manualUpNext,
                upcoming = state.normalUpNext
            )
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, PlayerQueue())

    // What the UI consumes — editing snapshot if active, else live projection
    val displayQueue: StateFlow<PlayerQueue> = combine(_uiState, playerQueue) { ui, live ->
        ui.editingQueue ?: live
    }.stateIn(viewModelScope, SharingStarted.Eagerly, playerQueue.value)

    // Shuffle and repeat state from QueueManager
    val isShuffled: StateFlow<Boolean> = queueManager.queueState
        .map { it.isShuffled }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val playbackMode: StateFlow<PlaybackMode> = queueManager.queueState
        .map { it.playbackMode }
        .stateIn(viewModelScope, SharingStarted.Eagerly, PlaybackMode.OFF)


    init {
        Log.d("logging", "viewmodel int")

        viewModelScope.launch {
            restorePlaybackState()

            // 2. Set up observers AFTER restoration completes
            // Sync queue changes to music player (for future changes)
            queueManager.intent
                .onEach { intent ->
                    // Access the current snapshot of the state directly
                    val state = queueManager.queueState.value
                    val queue = state.playbackQueue

                    if (queue.isEmpty()) return@onEach

                    when (intent) {
                        is QueueIntent.ReplaceQueue ->
                            musicPlayerManager.replaceFullQueueKeepingCurrentSong(queue, intent.newIndex)

                        is QueueIntent.SeekToItem -> {
                            musicPlayerManager.seekToDefaultPosition(intent.newIndex)
                        }

                        is QueueIntent.SeekToPreviousManual -> {
                            musicPlayerManager.seekToDefaultPosition(intent.newIndex)
                            musicPlayerManager.replaceFullQueueKeepingCurrentSong(queue, intent.newIndex + intent.offset)
                        }

                        is QueueIntent.NewQueue ->
                            musicPlayerManager.setPlaylist(queue, state.currentBaseIndex, 0L, state.autoPlay)
                    }

                    // Optional: If intent is a MutableStateFlow, reset it here to avoid re-processing
                    // on configuration changes if you aren't using a SharedFlow.
                    // queueManager.resetIntent()
                }
                .launchIn(viewModelScope)

            // Debounced save of queue state
            queueManager.queueState
                .debounce(500)
                .onEach { state -> savedDataRepository.saveQueueState(state) }
                .launchIn(viewModelScope)
        }
    }

    private suspend fun restorePlaybackState() {
        // 1. Restore state first (blocking in this coroutine)
        val savedState = savedDataRepository.getQueueState()
        savedState?.let { state ->
            val positionMs = savedDataRepository.getPosition() ?: 0L
            val playbackQueue = state.baseQueue
            if (playbackQueue.isNotEmpty()) {

                musicPlayerManager.setPlaylist(
                    playbackQueue,
                    startIndex = state.currentBaseIndex,
                    positionMs = positionMs,
                    autoPlay = false
                )
                Log.d("logging", "Restored playback position: $positionMs ms")
            }

            queueManager.restoreState(state)
            Log.d("logging", "Restored queue state: $state")
        } ?: Log.d("logging", "No saved queue state found")
    }

    // ── Playback ──────────────────────────────────────
    fun onPlayPauseClicked() {
        if (playerState.value.isPlaying) musicPlayerManager.pause()
        else musicPlayerManager.play()
    }

    fun onNextClicked() = queueManager.playNext()
    fun onPreviousClicked() = queueManager.playPrevious()
    fun onSeekTo(seconds: Long) = musicPlayerManager.seekTo(seconds)

    fun changeShuffleOption() {
        val state = queueManager.queueState.value
        if (state.isShuffled) {
            queueManager.unshuffle()
        } else {
            queueManager.shuffle()
        }
    }

    fun togglePlaybackMode() = queueManager.togglePlaybackMode()

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
        val state = queueManager.queueState.value

        // If song is already current, do nothing
        if (song.uniqueId == state.current?.uniqueId) return

        _uiState.update { it.copy(showHistory = false) }

        // Check if song is in history
        val historyIndex = state.history.indexOfFirst { it.uniqueId == song.uniqueId }
        if (historyIndex != -1) {
            queueManager.selectHistorySong(historyIndex)
            return
        }

        // Check if song is in manual queue
        val manualIndex = state.manualUpNext.indexOfFirst { it.uniqueId == song.uniqueId }
        if (manualIndex != -1) {
            queueManager.selectManualSong(manualIndex)
            return
        }

        // Check if song is in normal upcoming queue
        val normalIndex = state.normalUpNext.indexOfFirst { it.uniqueId == song.uniqueId }
        if (normalIndex != -1) {
            // Convert to absolute index in base queue
            val currentIndex = state.currentBaseIndex
            queueManager.selectNormalSong(currentIndex + 1 + normalIndex)
            return
        }

        // Song not found in queue
    }

    fun onMenuClicked(song: Song) {
        _uiState.update {
            it.copy(isMenuBottomSheetVisible = true, selectedSong = song)
        }
    }

    fun onCloseMenuBottomSheet() {
        _uiState.update {
            it.copy(isMenuBottomSheetVisible = false, selectedSong = null)
        }
    }

    fun addSelectedSongToQueue() {
        val song = _uiState.value.selectedSong ?: return
        queueManager.addToManualQueue(song)
        onCloseMenuBottomSheet()
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
        val combined = (current.manual + current.upcoming).toMutableList()
        val manualCount = current.manual.size

        val fromIndex = combined.indexOfFirst { it.uniqueId == fromKey }.takeIf { it != -1 } ?: return

        val toIndex = when (toKey) {
            "header_manual" -> 0
            "header_upcoming" -> manualCount  // drop on Queue header = start of upcoming
            "footer_repeat_mode" -> combined.size
            else -> combined.indexOfFirst { it.uniqueId == toKey }.takeIf { it != -1 } ?: return
        }

        combined.add(toIndex, combined.removeAt(fromIndex))

        val newManualCount = when {
            fromIndex < manualCount && toIndex >= manualCount -> manualCount - 1
            fromIndex >= manualCount && toIndex < manualCount -> manualCount + 1
            else -> manualCount
        }

        _uiState.update {
            it.copy(
                editingQueue = current.copy(
                    manual = combined.take(newManualCount),
                    upcoming = combined.drop(newManualCount)
                )
            )
        }
    }

    fun onDragEnd() {
        val editing = _uiState.value.editingQueue ?: return
        queueManager.replaceQueuesPreservingState(
            baseQueue = editing.history + listOfNotNull(editing.current) + editing.upcoming,
            manualQueue = editing.manual,
            currentBaseIndex = editing.history.size
        )
//        _uiState.update { it.copy(editingQueue = null, isEditingQueue = false) }
    }

    // ── Queue Management ──────────────────────────────

    fun removeSong(song: Song) {
        val state = queueManager.queueState.value

        // Check if song is in manual queue
        val manualIndex = state.manualUpNext.indexOfFirst { it.uniqueId == song.uniqueId }
        if (manualIndex != -1) {
            queueManager.removeManualSong(manualIndex)
            return
        }

        // Check if song is in normal upcoming queue
        val normalIndex = state.normalUpNext.indexOfFirst { it.uniqueId == song.uniqueId }
        if (normalIndex != -1) {
            val currentIndex = state.currentBaseIndex
            queueManager.removeNormalSong(currentIndex + 1 + normalIndex)
            return
        }

        // Song not found or is in history/current (can't remove those)
    }

}

data class MusicPlayerUiState(
    val isFullScreenVisible: Boolean = false,
    val showHistory: Boolean = false,
    val isEditingQueue: Boolean = false,
    val editingQueue: PlayerQueue? = null,
    val isMenuBottomSheetVisible: Boolean = false,
    val selectedSong: Song? = null
)

sealed interface MusicPlayerEffect {
    data object ScrollUpWhenHistoryOpened : MusicPlayerEffect
    data object ScrollToFirst : MusicPlayerEffect
}
