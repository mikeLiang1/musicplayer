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

class SongMenuViewModel constructor(
    private val playlistRepository: PlaylistRepository,
    private val queueManager: QueueManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(SongMenuState())
    val uiState = _uiState.asStateFlow()

    val playlists = playlistRepository.allPlaylists.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    fun onMenuClicked(song: Song) {
        _uiState.update {
            it.copy(
                isMenuSheetVisible = true,
                selectedSong = song,
                isManualSongSelected = queueManager.queueState.value.manualQueue.contains(song)
            )
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
                removeSong(song)
            }

            is SongMenuAction.RemoveFromPlaylist -> {
                val song = _uiState.value.selectedSong ?: return
                viewModelScope.launch {
                    playlistRepository.deleteSongFromPlaylist(playlistId = action.playlistId, songId = song.uniqueId)
                }
            }

            else -> {}
        }
    }

    fun removeSong(song: Song) {
        queueManager.removeSong(song.uniqueId)
    }

    fun addSongToSelectedPlaylist(playlistId: String) {
        val song = _uiState.value.selectedSong ?: return
        viewModelScope.launch {
            playlistRepository.addSongToPlaylist(playlistId, song)
        }
    }
}

data class SongMenuState(
    val isMenuSheetVisible: Boolean = false,
    val isPlaylistSheetVisible: Boolean = false,
    val selectedSong: Song? = null,
    val isManualSongSelected: Boolean = false
)
