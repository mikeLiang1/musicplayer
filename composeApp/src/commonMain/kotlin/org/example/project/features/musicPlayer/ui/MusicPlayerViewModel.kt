package org.example.project.features.musicPlayer.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import org.example.project.core.manager.MusicPlayerManager
import org.example.project.core.model.Song
import org.example.project.core.repository.YouTubeRepository

class MusicPlayerViewModel constructor(
    private val repository: YouTubeRepository,
    private val musicPlayerManager: MusicPlayerManager
) : ViewModel() {
    val uiState: StateFlow<MusicPlayerUiState> = combine(
        musicPlayerManager.isPlaying,
        musicPlayerManager.currentPosition,
        musicPlayerManager.duration,
        musicPlayerManager.currentSong
    ) { isPlaying, position, duration, song ->
        MusicPlayerUiState(
            isPlaying = isPlaying,
            duration = duration,
            currentSong = song
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MusicPlayerUiState()
    )

    val currentPosition = musicPlayerManager.currentPosition

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

}


data class MusicPlayerUiState(
    val currentSong: Song? = null,
    val isPlaying: Boolean = false,
    val duration: Long = 0L,
    val isLoading: Boolean = false
)

