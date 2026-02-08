package org.example.project.features.musicPlayer.ui

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.example.project.core.manager.MusicPlayerManager
import org.example.project.core.model.Song
import org.example.project.core.repository.YouTubeRepository

class MusicPlayerViewModel constructor(
    private val repository: YouTubeRepository,
    private val musicPlayerManager: MusicPlayerManager
) : ViewModel() {
    private val _uiState = MutableStateFlow(MusicPlayerUiState())
    val uiState = _uiState.asStateFlow()

    fun onPlayPressed() {
//        musicPlayerManager.start()
    }

}


data class MusicPlayerUiState(
    val isLoading: Boolean = false,
    val searchQuery: String = "",
    val songList: List<Song> = listOf(),
    val suggestions: List<String> = listOf()
)

