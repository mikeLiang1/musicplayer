package org.example.project.features.playlist.ui

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.example.project.core.manager.MusicPlayerManager
import org.example.project.core.manager.QueueManager
import org.example.project.core.model.Song
import org.example.project.core.repository.YouTubeRepository

class PlaylistViewModel constructor(
    private val playlistId: String,
    private val repository: YouTubeRepository,
    private val musicPlayerManager: MusicPlayerManager,
    private val queueManager: QueueManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlaylistUiState(playlistId = playlistId))
    val uiState = _uiState.asStateFlow()

}

data class PlaylistUiState(
    val playlistId: String,
    val songs: List<Song> = listOf()
)
