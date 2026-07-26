package org.example.project.features.likedSongs.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import org.example.project.core.manager.MusicPlayerManager
import org.example.project.core.manager.QueueManager
import org.example.project.core.model.Song
import org.example.project.features.playlist.repository.PlaylistRepository

private const val LIKED_SONGS_CONTEXT_ID = "liked_songs"

class LikedSongsViewModel(
    private val playlistRepository: PlaylistRepository,
    private val queueManager: QueueManager,
    private val musicPlayerManager: MusicPlayerManager
) : ViewModel() {

    val uiState: StateFlow<LikedSongsUiState> = combine(
        playlistRepository.getLikedSongs(),
        queueManager.queueState,
        musicPlayerManager.playerState
    ) { songs, queue, playerState ->
        LikedSongsUiState(
            songs = songs,
            isLoading = false,
            currentlyPlayingSongId = queue.current?.uniqueId,
            isContextActive = queue.contextId == LIKED_SONGS_CONTEXT_ID,
            isPlaying = playerState.isPlaying
        )
    }
        .onStart { emit(LikedSongsUiState(isLoading = true)) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = LikedSongsUiState(isLoading = true)
        )

    fun handleAction(action: LikedSongsAction) {
        val state = uiState.value

        when (action) {
            is LikedSongsAction.OnSongClicked -> {
                if (!state.isContextActive) {
                    queueManager.setBaseQueue(
                        songs = state.songs,
                        contextId = LIKED_SONGS_CONTEXT_ID,
                        currentBaseIndex = action.index
                    )
                }
                queueManager.playSongFromQueue(action.song.uniqueId)
            }

            LikedSongsAction.OnPlayPressed -> {
                if (state.songs.isEmpty()) return
                if (state.isContextActive) {
                    // Already our queue — the FAB is a plain play/pause toggle.
                    if (state.isPlaying) musicPlayerManager.pause() else musicPlayerManager.play()
                } else {
                    // setBaseQueue sets autoPlay and emits NewQueue, so this starts playback.
                    queueManager.setBaseQueue(
                        songs = state.songs,
                        contextId = LIKED_SONGS_CONTEXT_ID,
                        currentBaseIndex = 0
                    )
                }
            }

            LikedSongsAction.OnShufflePressed -> {
                if (state.songs.isEmpty()) return
                // Start on a random song, then shuffle() randomises everything after it —
                // otherwise shuffle-play would always open with the most recently liked song.
                queueManager.setBaseQueue(
                    songs = state.songs,
                    contextId = LIKED_SONGS_CONTEXT_ID,
                    currentBaseIndex = state.songs.indices.random()
                )
                queueManager.shuffle()
            }

        }
    }
}

data class LikedSongsUiState(
    val songs: List<Song> = emptyList(),
    val isLoading: Boolean = false,
    val currentlyPlayingSongId: String? = null,
    val isContextActive: Boolean = false,
    val isPlaying: Boolean = false
)

sealed interface LikedSongsAction {
    data class OnSongClicked(val song: Song, val index: Int) : LikedSongsAction
    data object OnPlayPressed : LikedSongsAction
    data object OnShufflePressed : LikedSongsAction
}
