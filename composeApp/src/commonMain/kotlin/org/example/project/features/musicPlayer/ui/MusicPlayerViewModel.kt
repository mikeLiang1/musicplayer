package org.example.project.features.musicPlayer.ui

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.example.project.core.manager.MusicPlayerManager
import org.example.project.core.model.Song
import org.example.project.core.repository.YouTubeRepository
import org.schabi.newpipe.extractor.timeago.patterns.it

class MusicPlayerViewModel constructor(
    private val repository: YouTubeRepository,
    private val musicPlayerManager: MusicPlayerManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(MusicPlayerUiState())
    val uiState = _uiState.asStateFlow()


    val playerState = musicPlayerManager.playerState

    val currentPosition = musicPlayerManager.currentPosition


    init {
        // Observe index changes from the manager and reset history when it changes
        viewModelScope.launch {
            playerState
                .map { it.currentIndex }
                .distinctUntilChanged() // only fires when index actually changes
                .collect { resetHistory() }
        }
    }

    fun onPlayPauseClicked() {
        if (playerState.value.isPlaying) musicPlayerManager.pause()
        else musicPlayerManager.play()
    }

    fun onNextClicked() {
        musicPlayerManager.skipToNext()
    }

    fun onPreviousClicked() {
        musicPlayerManager.skipToPrevious()
    }

    fun onSeekTo(seconds: Long) {
        musicPlayerManager.seekTo(seconds)
    }

    fun setFullScreen(fullScreen: Boolean) {
        _uiState.update { it.copy(isFullScreenVisible = fullScreen) }
    }

    fun onQueueClicked() {
        viewModelScope.launch {
            repository.getPlaylistRadio(playerState.value.currentSong?.url ?: "")

        }
    }

    fun changePlayingToIndex(index: Int, isInPreviousList: Boolean) {
        val realIndex = if (isInPreviousList)  index else playerState.value.currentIndex + index
        musicPlayerManager.seekToIndex(realIndex)
    }

    fun onAtTop() {
        // Only update if we aren't already AT_TOP and haven't revealed history yet
        if (_uiState.value.queueState == QueueState.LOCKED && !_uiState.value.showHistory) {
            _uiState.update { it.copy(queueState = QueueState.AT_TOP) }
        }
    }

    fun onScrolling() {
        // Only update if we were previously showing the Hint
        if (_uiState.value.queueState == QueueState.AT_TOP) {
            _uiState.update { it.copy(queueState = QueueState.LOCKED) }
        }
    }

    fun onRevealHistory() {
        _uiState.update { it.copy(queueState = QueueState.REVEALED, showHistory = true) }
    }

    private fun resetHistory() {
        _uiState.update { it.copy(queueState = QueueState.LOCKED, showHistory = false) }
    }
}


@Stable
data class MusicPlayerUiState(
    val isLoading: Boolean = false,
    val isFullScreenVisible: Boolean = false,
    val showHistory: Boolean = false,
    val queueState: QueueState = QueueState.LOCKED
)

enum class QueueState {
    LOCKED, AT_TOP, REVEALED
}

