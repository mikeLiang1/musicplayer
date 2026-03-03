package org.example.project.features.musicPlayer.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.example.project.core.manager.MusicPlayerManager
import org.example.project.core.model.Song
import org.example.project.core.repository.YouTubeRepository

class MusicPlayerViewModel constructor(
    private val repository: YouTubeRepository,
    private val musicPlayerManager: MusicPlayerManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(MusicPlayerUiState())
    val uiState = _uiState.asStateFlow()

    private val _effect = MutableSharedFlow<MusicPlayerEffect>()
    val effect: SharedFlow<MusicPlayerEffect> = _effect.asSharedFlow()

    val playerState = musicPlayerManager.playerState
    val currentPosition = musicPlayerManager.currentPosition

    // ── Playback ──────────────────────────────────────
    fun onPlayPauseClicked() {
        if (playerState.value.isPlaying) musicPlayerManager.pause()
        else musicPlayerManager.play()
    }

    fun onNextClicked() = musicPlayerManager.skipToNext()
    fun onPreviousClicked() = musicPlayerManager.skipToPrevious()
    fun onSeekTo(seconds: Long) = musicPlayerManager.seekTo(seconds)
    fun changeShuffleOption() = musicPlayerManager.shuffle()
    fun cycleFlowMode() = musicPlayerManager.cycleFlowMode()

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
                _effect.emit(MusicPlayerEffect.ScrollUp)
            }
        }
    }

    fun changePlayingToIndex(index: Int) {
        _uiState.update { it.copy(showHistory = false) }
        musicPlayerManager.seekToIndex(index)
    }

    // TODO Menu bottomsheet,
    fun onMenuClicked(song: Song) {
        musicPlayerManager.addToQueue(song)
    }

    // ── Queue Edit ────────────────────────────────────
    fun onEditQueueClicked() {
        if (_uiState.value.isEditingQueue) {
            _uiState.update { it.copy(isEditingQueue = false, editingQueue = emptyList()) }
        } else {
            val currentQueue = playerState.value.queue
            _uiState.update {
                it.copy(
                    isEditingQueue = true,
                    editingQueue = currentQueue
                )
            }
        }
    }

    fun onMove(fromIndex: Int, toIndex: Int) {
        if (!uiState.value.isEditingQueue) return

        val currentIndex = playerState.value.currentIndex

        // if showHistory, indices are already absolute, no offset needed
        val absoluteTo = (if (uiState.value.showHistory) toIndex
        else currentIndex + 1 + toIndex)
            .coerceAtLeast(currentIndex + 1) // can't drop before or at current

        val absoluteFrom = if (uiState.value.showHistory) fromIndex
        else currentIndex + 1 + fromIndex

        if (absoluteFrom == currentIndex) return

        val queue = uiState.value.editingQueue.toMutableList()
        if (absoluteFrom !in queue.indices || absoluteTo !in queue.indices) return

        val item = queue.removeAt(absoluteFrom)

        val manualQueueEnd = currentIndex + playerState.value.manualItemCount

        val crossing = when {
            !item.isManual && absoluteTo in (currentIndex + 1)..manualQueueEnd -> BucketCrossing.INTO_MANUAL
            item.isManual && absoluteTo > manualQueueEnd -> BucketCrossing.INTO_FUTURE
            else -> BucketCrossing.NONE
        }

        val updatedItem = when (crossing) {
            BucketCrossing.INTO_MANUAL -> item.copy(isManual = true)
            BucketCrossing.INTO_FUTURE -> item.copy(isManual = false)
            else -> item
        }

        queue.add(absoluteTo, updatedItem)
        _uiState.update { it.copy(editingQueue = queue) }
    }

    fun onDragEnd() {
        if (!uiState.value.isEditingQueue) return
        musicPlayerManager.moveSong(uiState.value.editingQueue)
    }

}

data class MusicPlayerUiState(
    val isFullScreenVisible: Boolean = false,
    val showHistory: Boolean = false,
    val isEditingQueue: Boolean = false,
    val editingQueue: List<Song> = listOf()
)

sealed interface MusicPlayerEffect {
    data object ScrollUp : MusicPlayerEffect
    data object ScrollToFirst : MusicPlayerEffect
}

enum class BucketCrossing {
    NONE,
    INTO_MANUAL,
    INTO_FUTURE,
    FROM_HISTORY
}

