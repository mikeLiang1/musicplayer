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
                                contextId = playlistId,
                                currentBaseIndex = playlistAction.index
                            )
                            recentlyPlaylistRepository.recordPlaylist(playlist)
                            playlistRepository.markPlayed(playlistId)
                        }
                    }

                    queueManager.playSongFromQueue(playlistAction.song.uniqueId)
                }
            }

            PlaylistAction.OnMenuPressed -> {

            }

            PlaylistAction.OnPlayPressed -> {
                val state = uiState.value
                val playlist = state.playlist ?: return
                if (playlist.songs.isEmpty()) return

                if (state.isPlaylistActive) {
                    // Already our queue — the FAB is a plain play/pause toggle.
                    if (state.isPlaying) musicPlayerManager.pause() else musicPlayerManager.play()
                } else {
                    viewModelScope.launch {
                        // setBaseQueue sets autoPlay and emits NewQueue, so this starts playback.
                        queueManager.setBaseQueue(
                            songs = playlist.songs.map { it.song },
                            contextId = playlistId,
                            currentBaseIndex = 0
                        )
                        recentlyPlaylistRepository.recordPlaylist(playlist)
                        playlistRepository.markPlayed(playlistId)
                    }
                }
            }

            PlaylistAction.OnSearchPressed -> {

            }

            PlaylistAction.OnShuffledPressed -> {
                val state = uiState.value
                val playlist = state.playlist ?: return
                if (playlist.songs.isEmpty()) return

                viewModelScope.launch {
                    // Start on a random song, then shuffle() randomises everything after it —
                    // otherwise shuffle-play would always open with the playlist's first track.
                    queueManager.setBaseQueue(
                        songs = playlist.songs.map { it.song },
                        contextId = playlistId,
                        currentBaseIndex = playlist.songs.indices.random()
                    )
                    queueManager.shuffle()
                    recentlyPlaylistRepository.recordPlaylist(playlist)
                    playlistRepository.markPlayed(playlistId)
                }
            }
        }
    }
}


sealed interface PlaylistAction {
    data object OnShuffledPressed : PlaylistAction
    data object OnMenuPressed : PlaylistAction
    data object OnPlayPressed : PlaylistAction
    data object OnSearchPressed : PlaylistAction
    data class OnPlaylistSongPressed(val song: Song, val index: Int) : PlaylistAction
}

data class PlaylistUiState(
    val playlist: Playlist? = null,
    val isLoading: Boolean = false,
    val currentlyPlayingSongId: String? = null,
    val isPlaylistActive: Boolean = false,
    val isPlaying: Boolean = false
)
