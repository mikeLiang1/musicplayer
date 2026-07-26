package org.example.project.features.songMenu.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.example.project.core.manager.QueueManager
import org.example.project.core.model.Song
import org.example.project.features.playlist.repository.PlaylistRepository

class SongMenuViewModel(
    private val playlistRepository: PlaylistRepository,
    private val queueManager: QueueManager
) : ViewModel() {
    private val _uiState = MutableStateFlow(SongMenuState())
    val uiState = _uiState.asStateFlow()

    val playlists = playlistRepository.getPlaylists()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Liked Songs is a permanent row in the add-to-playlist sheet, so the sheet needs its
    // count and whether the selected song is already in it — both live, so the row reflects
    // a like/unlike made from the sheet itself.
    val likedSongCount: StateFlow<Int> = playlistRepository.getLikedSongCount()
        .stateIn(viewModelScope, SharingStarted.Lazily, 0)

    @OptIn(ExperimentalCoroutinesApi::class)
    val isSelectedSongLiked: StateFlow<Boolean> = _uiState
        .map { it.selectedSong?.url }
        .distinctUntilChanged()
        .flatMapLatest { url ->
            if (url == null) flowOf(false) else playlistRepository.observeIsSongLiked(url)
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, false)

    fun onMenuClicked(song: Song, menuActions: List<SongMenuAction>, playlistSongId: String?) {
        _uiState.update {
            it.copy(
                isMenuSheetVisible = true,
                selectedSong = song,
                menuActions = withLikeAction(menuActions, song.isLiked),
                playlistSongId = playlistSongId
            )
        }
        // The passed-in Song may be stale/default on liked status (e.g. search results
        // never carry it) — confirm against the DB and correct the sheet in place.
        viewModelScope.launch {
            val liked = playlistRepository.isSongLiked(song.url)
            _uiState.update { current ->
                if (current.selectedSong?.uniqueId != song.uniqueId) return@update current
                current.copy(menuActions = withLikeAction(menuActions, liked))
            }
        }
    }

    private fun withLikeAction(base: List<SongMenuAction>, liked: Boolean): List<SongMenuAction> {
        val likeAction = if (liked) SongMenuAction.Unlike else SongMenuAction.Like
        return listOf(likeAction) + base
    }

    /** Opens the add-to-playlist sheet directly, skipping the song menu (e.g. the player heart). */
    fun onAddToPlaylistClicked(song: Song) {
        _uiState.update {
            it.copy(selectedSong = song, isPlaylistSheetVisible = true, playlistSongId = null)
        }
    }

    fun onCloseMenuSheet() {
        _uiState.update { it.copy(isMenuSheetVisible = false) }
    }

    fun onClosePlaylistSheet() {
        _uiState.update { it.copy(isPlaylistSheetVisible = false) }
    }

    fun handleAction(action: SongMenuAction) {
        when (action) {
            SongMenuAction.AddToPlaylist -> _uiState.update {
                it.copy(isPlaylistSheetVisible = true)
            }

            SongMenuAction.AddToQueue -> {
                val song = _uiState.value.selectedSong ?: return
                queueManager.addToManualQueue(song)
                onCloseMenuSheet()
            }

            SongMenuAction.RemoveFromQueue -> {
                val song = _uiState.value.selectedSong ?: return
                queueManager.removeSong(song.uniqueId)
            }

            is SongMenuAction.RemoveFromPlaylist -> {
                val playlistSongId = _uiState.value.playlistSongId ?: return
                viewModelScope.launch {
                    playlistRepository.removePlaylistSong(playlistSongId)
                }
            }

            SongMenuAction.GoToAlbum -> {}
            SongMenuAction.GoToArtist -> {}

            SongMenuAction.Like, SongMenuAction.Unlike -> {
                val song = _uiState.value.selectedSong ?: return
                viewModelScope.launch {
                    playlistRepository.toggleLike(song)
                }
                onCloseMenuSheet()
            }
        }
    }

    fun addSongToSelectedPlaylist(playlistId: String) {
        val song = _uiState.value.selectedSong ?: return
        viewModelScope.launch {
            playlistRepository.addSong(playlistId, song)
        }
    }

    /** Liked Songs row in the add-to-playlist sheet: acts as an add/remove toggle. */
    fun toggleLikeForSelectedSong() {
        val song = _uiState.value.selectedSong ?: return
        viewModelScope.launch {
            playlistRepository.toggleLike(song)
        }
    }
}

data class SongMenuState(
    val isMenuSheetVisible: Boolean = false,
    val isPlaylistSheetVisible: Boolean = false,
    val selectedSong: Song? = null,
    val menuActions: List<SongMenuAction> = emptyList(),
    val playlistSongId: String? = null
)
