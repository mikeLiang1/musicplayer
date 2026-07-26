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
import org.example.project.features.library.model.LibraryItem
import org.example.project.features.playlist.repository.PlaylistRepository

class LibraryViewModel(
    private val musicPlayerManager: MusicPlayerManager,
    private val queueManager: QueueManager,
    private val playlistRepository: PlaylistRepository
) : ViewModel() {

    private val _effect = MutableSharedFlow<LibraryEffect>()
    val effect: SharedFlow<LibraryEffect> = _effect.asSharedFlow()

    private val _selectedFilter = MutableStateFlow<LibraryItemFilter>(LibraryItemFilter.All)

    // Transient sheet state — not backed by Room, so it joins the combine as its own source
    private val _createPlaylist = MutableStateFlow(CreatePlaylistState())

    // 2. Combine Room data with the Filter flow
    val uiState: StateFlow<LibraryUiState> = combine(
        playlistRepository.getPlaylists(),
        _selectedFilter,
        playlistRepository.getLikedSongCount(),
        _createPlaylist
    ) { playlists, filter, likedSongCount, createPlaylist ->

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
            selectedFilter = filter,
            likedSongCount = likedSongCount,
            createPlaylist = createPlaylist
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
                val existingNames = uiState.value.allItems
                    .filterIsInstance<LibraryItem.PlaylistItem>()
                    .map { it.playlist.name }
                _createPlaylist.value = CreatePlaylistState(
                    isVisible = true,
                    name = suggestDefaultPlaylistName(existingNames)
                )
            }

            is LibraryAction.OnCreatePlaylistNameChanged -> {
                _createPlaylist.update { it.copy(name = libraryAction.name) }
            }

            LibraryAction.OnDismissCreatePlaylist -> {
                _createPlaylist.value = CreatePlaylistState()
            }

            LibraryAction.OnConfirmCreatePlaylist -> {
                val current = _createPlaylist.value
                val name = current.name.trim()
                // Guards a blank name and a double-tap on Create, which would otherwise
                // insert the playlist twice before the first insert closes the sheet.
                if (name.isEmpty() || current.isSaving) return
                _createPlaylist.update { it.copy(isSaving = true) }
                viewModelScope.launch {
                    val playlist = playlistRepository.createPlaylist(name)
                    _createPlaylist.value = CreatePlaylistState()
                    _effect.emit(LibraryEffect.NavigateToPlaylist(playlist.id))
                }
            }

            LibraryAction.OnLikedSongsClicked -> {
                viewModelScope.launch {
                    _effect.emit(LibraryEffect.NavigateToLikedSongs)
                }
            }
        }
    }
}

data class LibraryUiState(
    val allItems: List<LibraryItem> = listOf(),
    val likedSongCount: Int = 0,
    val selectedFilter: LibraryItemFilter = LibraryItemFilter.All,
    val libraryItems: List<LibraryItem> = listOf(),
    val createPlaylist: CreatePlaylistState = CreatePlaylistState()
)

data class CreatePlaylistState(
    val isVisible: Boolean = false,
    val name: String = "",
    val isSaving: Boolean = false
)


enum class LibraryItemFilter {
    All,
    Playlist
}

sealed interface LibraryAction {
    data class OnFilterSelected(val filter: LibraryItemFilter) : LibraryAction
    data class OnPlayListSelected(val playlistId: String) : LibraryAction
    data object OnAddPlaylist : LibraryAction
    data class OnCreatePlaylistNameChanged(val name: String) : LibraryAction
    data object OnConfirmCreatePlaylist : LibraryAction
    data object OnDismissCreatePlaylist : LibraryAction
    data object OnLikedSongsClicked : LibraryAction
}

sealed interface LibraryEffect {
    data class NavigateToPlaylist(val playlistId: String) : LibraryEffect
    data object NavigateToLikedSongs : LibraryEffect
}

