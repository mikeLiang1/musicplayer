package org.example.project.features.playlist.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import org.example.project.core.model.Playlist
import org.example.project.features.playlist.repository.PlaylistRepository

class PlaylistViewModel constructor(
    private val playlistId: String,
    private val playlistRepository: PlaylistRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlaylistUiState(playlistId = playlistId))

    val uiState: StateFlow<PlaylistUiState> = combine(
        playlistRepository.getSongsFromPlaylist(playlistId),
        _uiState
    ) { playlist, uiState ->
        uiState.copy(playlist = playlist, isLoading = uiState.isLoading)

    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PlaylistUiState(
            isLoading = true,
            playlistId = playlistId,
        )
    )
}

data class PlaylistUiState(
    val playlistId: String,
    val playlist: Playlist? = null,
    val isLoading: Boolean = false
)
