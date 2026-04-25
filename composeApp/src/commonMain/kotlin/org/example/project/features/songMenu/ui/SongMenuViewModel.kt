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

    data class SongMenuState(
        val isMenuSheetVisible: Boolean = false,
        val isPlaylistSheetVisible: Boolean = false,
        val selectedSong: Song? = null,
        val isManualSongSelected: Boolean = false
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

    fun handleAction(action: BottomSheetAction) {
        when (action) {
            BottomSheetAction.AddToPlaylist -> _uiState.update {
                it.copy(isPlaylistSheetVisible = true)
            }

            BottomSheetAction.AddToQueue -> {
                val song = _uiState.value.selectedSong ?: return
                queueManager.addToManualQueue(song)
                onCloseMenuSheet()
            }

            BottomSheetAction.RemoveFromQueue -> {
                val song = _uiState.value.selectedSong ?: return
                removeSong(song)
            }

            else -> {}
        }
    }

    fun removeSong(song: Song) {
        val state = queueManager.queueState.value

        // Check if song is in manual queue
        val manualIndex = state.manualUpNext.indexOfFirst { it.uniqueId == song.uniqueId }
        if (manualIndex != -1) {
            queueManager.removeManualSong(manualIndex)
            return
        }

        // Check if song is in normal upcoming queue
        val normalIndex = state.normalUpNext.indexOfFirst { it.uniqueId == song.uniqueId }
        if (normalIndex != -1) {
            val currentIndex = state.currentBaseIndex
            queueManager.removeNormalSong(currentIndex + 1 + normalIndex)
            return
        }

        // Song not found or is in history/current (can't remove those)
    }

    fun addSongToSelectedPlaylist(playlistId: String) {
        val song = _uiState.value.selectedSong ?: return
        viewModelScope.launch {
            playlistRepository.addSongToPlaylist(playlistId, song)
        }
    }
}
