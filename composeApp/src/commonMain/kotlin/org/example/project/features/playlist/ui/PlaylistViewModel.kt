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
    private val _uiState = MutableStateFlow(PlaylistUiState(isLoading = true))

    // TODO i think may need to seperate playlist and isloading since they arent related to each other
    val uiState: StateFlow<PlaylistUiState> = combine(
        playlistRepository.getPlaylist(playlistId),
        _uiState
    ) { playlist, uiState ->
        uiState.copy(playlist = playlist, isLoading = false)

    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PlaylistUiState(isLoading = true)
    )

    fun handleAction(playlistAction: PlaylistAction) {
        when (playlistAction) {
            PlaylistAction.OnShuffledPressed -> {}
            PlaylistAction.OnMenuPressed -> {

            }
            PlaylistAction.OnPlayPressed -> {

            }
            PlaylistAction.OnSearchPressed -> {

            }
        }
    }
}

sealed interface PlaylistAction {
    data object OnShuffledPressed : PlaylistAction
    data object OnMenuPressed : PlaylistAction
    data object OnPlayPressed : PlaylistAction
    data object OnSearchPressed : PlaylistAction
}

data class PlaylistUiState(
    val playlist: Playlist? = null,
    val isLoading: Boolean = false
)
