package org.example.project.features.songMenu.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
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
}

data class SongMenuState(
    val isMenuSheetVisible: Boolean = false,
    val isPlaylistSheetVisible: Boolean = false,
    val selectedSong: Song? = null,
    val menuActions: List<SongMenuAction> = emptyList(),
    val playlistSongId: String? = null
)
