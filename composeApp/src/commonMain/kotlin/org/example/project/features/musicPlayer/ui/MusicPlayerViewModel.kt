package org.example.project.features.musicPlayer.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.example.project.core.manager.MusicPlayerManager
import org.example.project.core.model.Song
import org.example.project.core.repository.QueueRepository
import org.example.project.core.repository.YouTubeRepository

class MusicPlayerViewModel constructor(
    private val repository: YouTubeRepository,
    private val musicPlayerManager: MusicPlayerManager,
    private val queueRepository: QueueRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MusicPlayerUiState())

    val uiState = combine(
        _uiState,
        queueRepository.queue,
        queueRepository.isShuffle
    ) { uiState, queue, isShuffle ->
        uiState.copy(queue = queue, isShuffle = isShuffle)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MusicPlayerUiState()
    )

    private val _effect = MutableSharedFlow<MusicPlayerEffect>()
    val effect: SharedFlow<MusicPlayerEffect> = _effect.asSharedFlow()

    val playerState = musicPlayerManager.playerState
    val currentPosition = musicPlayerManager.currentPosition

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

    fun onMenuClicked() {

    }

    fun changePlayingToIndex(index: Int) {
        musicPlayerManager.seekToIndex(index)
    }
    fun changeHistory(value: Boolean) {
        _uiState.update { it.copy(showHistory = value) }
    }

    fun scrollWhenHistoryOpened () {
        viewModelScope.launch {
            _effect.emit(MusicPlayerEffect.ScrollUp)
        }
    }

    fun changeShuffleOption() {
        val queueResult = queueRepository.shuffleClicked(playerState.value.currentIndex)
        if (queueRepository.isShuffle.value) {
            musicPlayerManager.replaceQueueKeepingCurrentSong(
                queueResult.queue,
                queueResult.currentIndex
            )
        }else {
            musicPlayerManager.replaceFullQueueKeepingCurrentSong(queueResult.queue, queueResult.currentIndex)
        }
    }

}

data class MusicPlayerUiState(
    val isFullScreenVisible: Boolean = false,
    val showHistory: Boolean = false,
    val visibleStartIndex: Int = 0,
    val queue: List<Song> = listOf(),
    val isShuffle: Boolean = false
)

sealed interface MusicPlayerEffect {
    data object ScrollUp : MusicPlayerEffect
}

