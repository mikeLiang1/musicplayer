package org.example.project.features.library.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.example.project.core.manager.MusicPlayerManager
import org.example.project.core.manager.QueueManager
import org.example.project.core.model.Playlist
import org.example.project.core.model.Song
import org.example.project.core.repository.YouTubeRepository
import org.example.project.features.library.model.LibraryItem

class LibraryViewModel constructor(
    private val repository: YouTubeRepository,
    private val musicPlayerManager: MusicPlayerManager,
    private val queueManager: QueueManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState = _uiState.asStateFlow()

    private val _effect = MutableSharedFlow<LibraryEffect>()
    val effect: SharedFlow<LibraryEffect> = _effect.asSharedFlow()

    fun handleAction(libraryAction: LibraryAction) {
        when (libraryAction) {
            is LibraryAction.OnFilterSelected -> {
                filterItems(libraryAction.filter)
            }
            is LibraryAction.OnPlayListSelected -> {
                viewModelScope.launch {
                    _effect.emit(LibraryEffect.NavigateToPlaylist(libraryAction.playlistId))
                }
            }
        }
    }

    private fun filterItems(filter: LibraryItemFilter) {
        _uiState.update { currentState ->
            val filteredList = when (filter) {
                LibraryItemFilter.All -> currentState.allItems
                LibraryItemFilter.Playlist -> currentState.allItems.filterIsInstance<LibraryItem.PlaylistItem>()
                LibraryItemFilter.Song -> currentState.allItems.filterIsInstance<LibraryItem.SongItem>()
            }

            currentState.copy(
                selectedFilter = filter,
                libraryItems = filteredList
            )
        }
    }
}

data class LibraryUiState(
    val allItems: List<LibraryItem> = listOf(
        LibraryItem.PlaylistItem(playlist = Playlist(title = "sad", thumbnailUrl = null, numSongs = 30)),
        LibraryItem.PlaylistItem(playlist = Playlist(title = "sad2", thumbnailUrl = null, numSongs = 30)),
        LibraryItem.PlaylistItem(playlist = Playlist(title = "sad3", thumbnailUrl = null, numSongs = 30)),
        LibraryItem.PlaylistItem(playlist = Playlist(title = "sad4", thumbnailUrl = null, numSongs = 30)),
        LibraryItem.SongItem(
            song = Song(
                url = "item.url",
                title = "Currently Playing Song",
                artist = "Artist",
                thumbnailUrl = "item.thumbnails.firstOrNull()?.url",
                duration = 3000L
            ),
        ),
        LibraryItem.SongItem(
            song = Song(
                url = "item.url",
                title = "Currently Playing Song2",
                artist = "Artist",
                thumbnailUrl = "item.thumbnails.firstOrNull()?.url",
                duration = 3000L
            ),
        ),
        LibraryItem.PlaylistItem(playlist = Playlist(title = "sad", thumbnailUrl = null, numSongs = 30)),
        LibraryItem.SongItem(
            song = Song(
                url = "item.url",
                title = "Currently Playing Song3",
                artist = "Artist",
                thumbnailUrl = "item.thumbnails.firstOrNull()?.url",
                duration = 3000L
            ),
        )
    ),
    val likedSongCount: Int = 0,
    val selectedFilter: LibraryItemFilter = LibraryItemFilter.All,
    val libraryItems: List<LibraryItem> = listOf(
        LibraryItem.PlaylistItem(playlist = Playlist(title = "sad", thumbnailUrl = null, numSongs = 30)),
        LibraryItem.PlaylistItem(playlist = Playlist(title = "sad2", thumbnailUrl = null, numSongs = 30)),
        LibraryItem.PlaylistItem(playlist = Playlist(title = "sad3", thumbnailUrl = null, numSongs = 30)),
        LibraryItem.SongItem(
            song = Song(
                url = "item.url",
                title = "Currently Playing Song",
                artist = "Artist",
                thumbnailUrl = "item.thumbnails.firstOrNull()?.url",
                duration = 3000L
            ),
        ),
        LibraryItem.SongItem(
            song = Song(
                url = "item.url",
                title = "Currently Playing Song2",
                artist = "Artist",
                thumbnailUrl = "item.thumbnails.firstOrNull()?.url",
                duration = 3000L
            ),
        ),
        LibraryItem.PlaylistItem(playlist = Playlist(title = "sad", thumbnailUrl = null, numSongs = 30)),
        LibraryItem.SongItem(
            song = Song(
                url = "item.url",
                title = "Currently Playing Song3",
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
    data class OnPlayListSelected(val playlistId: String) : LibraryAction
}

sealed interface LibraryEffect {
    data class NavigateToPlaylist(val playlistId: String) : LibraryEffect
}

