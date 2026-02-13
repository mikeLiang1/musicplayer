package org.example.project.features.musicPlayer.ui

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
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

    init {
        viewModelScope.launch {

            musicPlayerManager.currentPosition.collect { cur ->
                _uiState.update { it.copy(currentPos = cur) }
            }
        }
    }

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

    fun onQueueClicked() {
        viewModelScope.launch {
            repository.getPlaylistRadio(playerState.value.currentSong?.url ?: "")

        }
    }
    init {
//        viewModelScope.launch {
//            musicPlayerManager.isPlaying.collect { value ->
//                _uiState.update { currentState ->
//                    currentState.copy(isPlaying = value)
//                }
//            }
//        }
//
//        viewModelScope.launch {
//            musicPlayerManager.queue.collect {queue->
//                _uiState.update { it.copy(queue = queue) }
//            }
//        }
//
//        viewModelScope.launch {
//            musicPlayerManager.currentSong.collect { song ->
//                _uiState.update { it.copy(currentSong = song) }
//            }
//        }
//
//        viewModelScope.launch {
//            musicPlayerManager.duration.collect { duration ->
//                _uiState.update { it.copy(duration = duration) }
//            }
//        }
    }
}


@Stable
data class MusicPlayerUiState(
    val isLoading: Boolean = false,
    val isFullScreenVisible: Boolean = false,
    val currentPos: Long = 0L
)

