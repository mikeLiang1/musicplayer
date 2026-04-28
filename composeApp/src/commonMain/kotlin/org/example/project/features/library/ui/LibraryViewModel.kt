package org.example.project.features.library.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.example.project.core.manager.MusicPlayerManager
import org.example.project.core.manager.QueueManager
import org.example.project.core.model.Playlist
import org.example.project.core.repository.YouTubeRepository
import org.example.project.features.library.model.LibraryItem
import org.example.project.features.playlist.repository.PlaylistRepository
import org.schabi.newpipe.extractor.timeago.patterns.it

class LibraryViewModel constructor(
    private val repository: YouTubeRepository,
    private val musicPlayerManager: MusicPlayerManager,
    private val queueManager: QueueManager,
    private val playlistRepository: PlaylistRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LibraryUiState())
//    val uiState = _uiState.asStateFlow()

    private val _effect = MutableSharedFlow<LibraryEffect>()
    val effect: SharedFlow<LibraryEffect> = _effect.asSharedFlow()

    private val _selectedFilter = MutableStateFlow<LibraryItemFilter>(LibraryItemFilter.All)

    // 2. Combine Room data with the Filter flow
    val uiState: StateFlow<LibraryUiState> = combine(
        playlistRepository.getPlaylists(),
        _selectedFilter
    ) { playlists, filter ->

        // This block runs whenever EITHER the database changes OR the filter changes

        val allItems = playlists.map { LibraryItem.PlaylistItem(it) }

        val filteredList = when (filter) {
            LibraryItemFilter.All -> allItems
            LibraryItemFilter.Playlist -> allItems.filterIsInstance<LibraryItem.PlaylistItem>()
            // Add other filters here (e.g. Song)
        }

        LibraryUiState(
            allItems = allItems,
            libraryItems = filteredList,
            selectedFilter = filter
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = LibraryUiState() // Set a loading state initially
    )

    fun handleAction(libraryAction: LibraryAction) {
        when (libraryAction) {
            is LibraryAction.OnFilterSelected -> {
                _selectedFilter.update { libraryAction.filter }
            }

            is LibraryAction.OnPlayListSelected -> {
                viewModelScope.launch {
                    _effect.emit(LibraryEffect.NavigateToPlaylist(libraryAction.playlistId))
                }
            }

            LibraryAction.OnAddPlaylist -> {
                viewModelScope.launch {
                    playlistRepository.createPlaylist("playlist")
                }
            }
        }
    }
}

data class LibraryUiState(
    val allItems: List<LibraryItem> = listOf(),
    val likedSongCount: Int = 0,
    val selectedFilter: LibraryItemFilter = LibraryItemFilter.All,
    val libraryItems: List<LibraryItem> = listOf()
)


enum class LibraryItemFilter {
    All,
    Playlist
}

sealed interface LibraryAction {
    data class OnFilterSelected(val filter: LibraryItemFilter) : LibraryAction
    data class OnPlayListSelected(val playlistId: String) : LibraryAction
    data object OnAddPlaylist : LibraryAction
}

sealed interface LibraryEffect {
    data class NavigateToPlaylist(val playlistId: String) : LibraryEffect
}

