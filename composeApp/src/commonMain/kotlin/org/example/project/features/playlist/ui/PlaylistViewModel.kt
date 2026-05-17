package org.example.project.features.playlist.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.example.project.core.manager.MusicPlayerManager
import org.example.project.core.manager.QueueManager
import org.example.project.core.model.Playlist
import org.example.project.core.model.Song
import org.example.project.core.repository.RecentlyPlayedRepository
import org.example.project.features.playlist.repository.PlaylistRepository

class PlaylistViewModel(
    private val playlistId: String,
    private val playlistRepository: PlaylistRepository,
    private val recentlyPlaylistRepository: RecentlyPlayedRepository,
    private val queueManager: QueueManager,
    private val musicPlayerManager: MusicPlayerManager
) : ViewModel() {


    val uiState: StateFlow<PlaylistUiState> = combine(
        playlistRepository.getPlaylist(playlistId),
        queueManager.queueState,
        musicPlayerManager.playerState
    ) { playlist, queue, playerState ->

        PlaylistUiState(
            playlist = playlist,
            isLoading = false,
            currentlyPlayingSongId = queue.current?.uniqueId,
            isPlaylistActive = queue.contextId == playlistId,
            isPlaying = playerState.isPlaying
        )
    }
        .onStart { emit(PlaylistUiState(isLoading = true)) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = PlaylistUiState(isLoading = true)
        )

    fun handleAction(playlistAction: PlaylistAction) {
        when (playlistAction) {
            is PlaylistAction.OnPlaylistSongPressed -> {
                viewModelScope.launch {
                    val state = uiState.value

                    if (!state.isPlaylistActive) {
                        state.playlist?.let { playlist ->
                            queueManager.setBaseQueue(
                                songs = playlist.songs.map { it.song },
                                contextId = playlistId
                            )
                            recentlyPlaylistRepository.recordPlaylist(playlist)
                        }
                    }

                    queueManager.playSongFromQueue(playlistAction.song.uniqueId)
                }
            }

            PlaylistAction.OnMenuPressed -> {

            }

            PlaylistAction.OnPlayPressed -> {

            }

            PlaylistAction.OnSearchPressed -> {

            }

            PlaylistAction.OnShuffledPressed -> {

            }
        }
    }
}


sealed interface PlaylistAction {
    data object OnShuffledPressed : PlaylistAction
    data object OnMenuPressed : PlaylistAction
    data object OnPlayPressed : PlaylistAction
    data object OnSearchPressed : PlaylistAction
    data class OnPlaylistSongPressed(val song: Song) : PlaylistAction
}

data class PlaylistUiState(
    val playlist: Playlist? = null,
    val isLoading: Boolean = false,
    val currentlyPlayingSongId: String? = null,
    val isPlaylistActive: Boolean = false,
    val isPlaying: Boolean = false
)
