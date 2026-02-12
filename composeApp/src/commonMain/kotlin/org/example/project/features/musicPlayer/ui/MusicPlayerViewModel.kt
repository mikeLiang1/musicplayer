package org.example.project.features.musicPlayer.ui

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

    // Directly expose constantly changing state flow
    val currentPosition = musicPlayerManager.currentPosition

    init {
        viewModelScope.launch {
            musicPlayerManager.isPlaying.collect { value ->
                _uiState.update { currentState ->
                    currentState.copy(isPlaying = value)
                }
            }
        }

        viewModelScope.launch {
            musicPlayerManager.currentSong.collect { song ->
                _uiState.update { it.copy(currentSong = song) }
            }
        }

        viewModelScope.launch {
            musicPlayerManager.duration.collect { duration ->
                _uiState.update { it.copy(duration = duration) }
            }
        }
    }

    fun onPlayPauseClicked() {
        if (uiState.value.isPlaying) musicPlayerManager.pause()
        else musicPlayerManager.play()
    }

    fun onNextClicked() {

    }

    fun onPreviousClicked() {

    }

    fun onSeekTo(seconds: Long) {
        musicPlayerManager.seekTo(seconds)
    }

    fun setFullScreen(fullScreen: Boolean) {
        _uiState.update { it.copy(isFullScreenVisible = fullScreen) }
    }

}


data class MusicPlayerUiState(
    val currentSong: Song? = null,
    val isPlaying: Boolean = false,
    val duration: Long = 0L,
    val isLoading: Boolean = false,
    val isFullScreenVisible: Boolean = false
)

