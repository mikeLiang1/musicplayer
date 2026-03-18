//package org.example.project.features.musicPlayer.ui
//
//import androidx.lifecycle.ViewModel
//import androidx.lifecycle.viewModelScope
//import kotlinx.coroutines.flow.MutableSharedFlow
//import kotlinx.coroutines.flow.MutableStateFlow
//import kotlinx.coroutines.flow.SharedFlow
//import kotlinx.coroutines.flow.SharingStarted
//import kotlinx.coroutines.flow.StateFlow
//import kotlinx.coroutines.flow.asSharedFlow
//import kotlinx.coroutines.flow.asStateFlow
//import kotlinx.coroutines.flow.combine
//import kotlinx.coroutines.flow.filter
//import kotlinx.coroutines.flow.map
//import kotlinx.coroutines.flow.stateIn
//import kotlinx.coroutines.flow.update
//import kotlinx.coroutines.launch
//import org.example.project.core.manager.MusicPlayerManager
//import org.example.project.core.model.Song
//import org.example.project.core.repository.YouTubeRepository
//import org.example.project.features.musicPlayer.model.PlayerQueue
//
//class MusicPlayerViewModel constructor(
//    private val repository: YouTubeRepository,
//    private val musicPlayerManager: MusicPlayerManager
//) : ViewModel() {
//
//    private val _uiState = MutableStateFlow(MusicPlayerUiState())
//    val uiState = _uiState.asStateFlow()
//
//    private val _effect = MutableSharedFlow<MusicPlayerEffect>()
//    val effect: SharedFlow<MusicPlayerEffect> = _effect.asSharedFlow()
//
//    val playerState = musicPlayerManager.playerState
//    val currentPosition = musicPlayerManager.currentPosition
//
//    // Projection — ViewModel only, manager never sees this
//    val playerQueue: StateFlow<PlayerQueue> = playerState
//        .filter { it.queue.isNotEmpty() && it.currentIndex in it.queue.indices }
//        .map { PlayerQueue.from(it) }
//        .stateIn(viewModelScope, SharingStarted.Eagerly, PlayerQueue())
//
//
//    // What the UI consumes — editing snapshot if active, else live projection
//    val displayQueue: StateFlow<PlayerQueue> = combine(_uiState, playerQueue) { ui, live ->
//        ui.editingQueue ?: live
//    }.stateIn(viewModelScope, SharingStarted.Eagerly, playerQueue.value)
//
//    // ── Playback ──────────────────────────────────────
//    fun onPlayPauseClicked() {
//        if (playerState.value.isPlaying) musicPlayerManager.pause()
//        else musicPlayerManager.play()
//    }
//
//    fun onNextClicked() = musicPlayerManager.skipToNext()
//    fun onPreviousClicked() = musicPlayerManager.skipToPrevious()
//    fun onSeekTo(seconds: Long) = musicPlayerManager.seekTo(seconds)
//    fun changeShuffleOption() = musicPlayerManager.shuffle()
//    fun cycleFlowMode() = musicPlayerManager.cycleFlowMode()
//
//    // ── UI State ──────────────────────────────────────
//    fun setFullScreen(fullScreen: Boolean) {
//        _uiState.update { it.copy(isFullScreenVisible = fullScreen) }
//    }
//
//    fun onHistoryPillClicked() {
//        viewModelScope.launch {
//            if (uiState.value.showHistory) {
//                _uiState.update { it.copy(showHistory = false) }
//                _effect.emit(MusicPlayerEffect.ScrollToFirst)
//            } else {
//                _uiState.update { it.copy(showHistory = true) }
//                _effect.emit(MusicPlayerEffect.ScrollUpWhenHistoryOpened)
//            }
//        }
//    }
//
//    fun changePlayingToSong(song: Song) {
//        val absoluteIndex = playerQueue.value.absoluteIndexOf(song)
//        if (absoluteIndex == -1) return
//        _uiState.update { it.copy(showHistory = false) }
//        musicPlayerManager.seekToIndex(absoluteIndex)
//    }
//
//    fun onMenuClicked(song: Song) {
//        musicPlayerManager.addToQueue(song)
//    }
//
//    // ── Queue Edit ────────────────────────────────────
//    fun onEditQueueClicked() {
//        _uiState.update {
//            it.copy(
//                editingQueue = if (it.editingQueue != null) null else playerQueue.value,
//                isEditingQueue = !_uiState.value.isEditingQueue
//            )
//        }
//    }
//
//    fun onMove(fromKey: String, toKey: String) {
//        val current = _uiState.value.editingQueue ?: return
//        val future = (current.manual + current.upcoming).toMutableList()
//
//        val fromIndex = future.indexOfFirst { it.uniqueId == fromKey }.takeIf { it != -1 } ?: return
//        val toIndex = future.indexOfFirst { it.uniqueId == toKey }.takeIf { it != -1 } ?: return
//
//        val item = future.removeAt(fromIndex)
//        val updatedItem = when {
//            !item.isManual && toIndex < current.manual.size -> item.copy(isManual = true)
//            item.isManual && toIndex >= current.manual.size -> item.copy(isManual = false)
//            else -> item
//        }
//        future.add(toIndex, updatedItem)
//
//        // Recount from the final list state, don't derive from stale size
//        val newManualCount = future.count { it.isManual }
//
//        _uiState.update {
//            it.copy(
//                editingQueue = current.copy(
//                    manual = future.take(newManualCount),
//                    upcoming = future.drop(newManualCount)
//                )
//            )
//        }
//    }
//
//
//    fun onDragEnd() {
//        val editing = _uiState.value.editingQueue ?: return
//        // Reconstruct flat queue and hand back to manager
//        musicPlayerManager.moveSong(editing.allSongs)
//    }
//}
//
//data class MusicPlayerUiState(
//    val isFullScreenVisible: Boolean = false,
//    val showHistory: Boolean = false,
//    val isEditingQueue: Boolean = false,
//    val editingQueue: PlayerQueue? = null
//)
//
//sealed interface MusicPlayerEffect {
//    data object ScrollUpWhenHistoryOpened : MusicPlayerEffect
//    data object ScrollToFirst : MusicPlayerEffect
//}
//
