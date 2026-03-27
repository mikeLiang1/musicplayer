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
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.example.project.core.manager.MusicPlayerManager
import org.example.project.core.manager.QueueManager
import org.example.project.core.manager.RepeatMode
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

    val repeatMode: StateFlow<RepeatMode> = queueManager.queueState
        .map { it.repeatMode }
        .stateIn(viewModelScope, SharingStarted.Eagerly, RepeatMode.OFF)

    init {
        var lastCurrentId: String? = null

        // Sync queue changes to music player
        queueManager.queueState
            .onEach { state ->
                val playbackQueue = state.playbackQueue
                if (playbackQueue.isEmpty()) return@onEach

                val currentSong = state.current
                val currentId = currentSong?.uniqueId
                val startIndex = playbackQueue.indexOfFirst { it.uniqueId == currentSong?.uniqueId }.coerceAtLeast(0)

                if (currentId != lastCurrentId) {
                    // Song changed — full rebuild with new start position
                    lastCurrentId = currentId
                    musicPlayerManager.setPlaylist(playbackQueue, startIndex = 0, positionMs = 0L, autoPlay = false)
                } else {
                    // Same song, queue order changed — surgical update
                    musicPlayerManager.replaceFullQueueKeepingCurrentSong(playbackQueue, newIndex = startIndex)
                }
            }
            .launchIn(viewModelScope)

        // Debounced save of queue state
        queueManager.queueState
            .debounce(500)
            .onEach { state -> savedDataRepository.saveQueueState(state) }
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
        val state = queueManager.queueState.value

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
        val future = (current.manual + current.upcoming).toMutableList()
        val manualCount = current.manual.size

        val fromIndex = future.indexOfFirst { it.uniqueId == fromKey }.takeIf { it != -1 } ?: return
        val toIndex = future.indexOfFirst { it.uniqueId == toKey }.takeIf { it != -1 } ?: return

        val item = future.removeAt(fromIndex)
        future.add(toIndex, item)

        // Determine new manual/normal split based on position
        // Songs at indices 0 until manualCount are manual, rest are normal
        // But the count may shift if a song crossed the boundary
        val newManualCount = when {
            fromIndex < manualCount && toIndex >= manualCount -> manualCount - 1  // manual → normal
            fromIndex >= manualCount && toIndex < manualCount -> manualCount + 1  // normal → manual
            else -> manualCount  // same section
        }

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
        queueManager.replaceQueuesPreservingState(
            baseQueue = editing.history + listOfNotNull(editing.current) + editing.upcoming,
            manualQueue = editing.manual,
            currentBaseIndex = editing.history.size
        )
        _uiState.update { it.copy(editingQueue = null, isEditingQueue = false) }
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
