package org.example.project.features.home.ui

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.example.project.core.model.Song
import org.example.project.core.model.mockSongList
import org.example.project.core.repository.YouTubeRepository

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class HomeViewModel constructor(
    private val repository: YouTubeRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState = _uiState.asStateFlow()

    fun onHomeAction(action: HomeAction) {
        when(action) {
            is HomeAction.OnRecentPlayedClicked -> {

            }
        }
    }

}


data class HomeUiState(
    val isLoading: Boolean = false,
    // TOOD: Do we make this sealed interface like library item ?
    val recentlyPlayed: List<Song> = mockSongList
)

sealed interface HomeAction {
    data class OnRecentPlayedClicked(val song: Song): HomeAction
}
