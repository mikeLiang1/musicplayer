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
        when (action) {
            is LikedSongsAction.OnSongClicked -> {
                val state = uiState.value
                if (!state.isContextActive) {
                    queueManager.setBaseQueue(
                        songs = state.songs,
                        contextId = LIKED_SONGS_CONTEXT_ID,
                        currentBaseIndex = action.index
                    )
                }
                queueManager.playSongFromQueue(action.song.uniqueId)
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
}
