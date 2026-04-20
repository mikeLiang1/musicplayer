package org.example.project.features.library.ui

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.example.project.core.manager.MusicPlayerManager
import org.example.project.core.manager.QueueManager
import org.example.project.core.model.Playlist
import org.example.project.core.model.Song
import org.example.project.core.repository.YouTubeRepository
import org.example.project.features.library.model.LibraryItem

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class LibraryViewModel constructor(
    private val repository: YouTubeRepository,
    private val musicPlayerManager: MusicPlayerManager,
    private val queueManager: QueueManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState = _uiState.asStateFlow()


    fun handleAction(libraryAction: LibraryAction) {
        when (libraryAction) {
            is LibraryAction.OnFilterSelected -> {
                _uiState.update { it.copy(selectedFilter = libraryAction.filter) }
            }
        }
    }
}


data class LibraryUiState(
    val likedSongCount: Int = 0,
    val selectedFilter: LibraryItemFilter = LibraryItemFilter.All,
    val libraryItems: List<LibraryItem> = listOf(
        LibraryItem.PlaylistItem(playlist = Playlist(title = "sad", thumbnailUrl = null, numSongs = 30)),
        LibraryItem.SongItem(
            song = Song(
                url = "item.url",
                title = "Currently Playing Song",
                artist = "Artist",
                thumbnailUrl = "item.thumbnails.firstOrNull()?.url",
                duration = 3000L
            ),
        )
    )
)



enum class LibraryItemFilter {
    All,
    Playlist,
    Song
}

sealed interface LibraryAction {
    data class OnFilterSelected(val filter: LibraryItemFilter) : LibraryAction
}


