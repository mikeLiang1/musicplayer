package org.example.project.features.home.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.example.project.core.database.mapper.toRecentlyPlayedItem
import org.example.project.core.model.RecentlyPlayedItem
import org.example.project.core.model.Song
import org.example.project.core.model.mockSongList
import org.example.project.core.repository.RecentlyPlayedRepository
import org.example.project.core.repository.YouTubeRepository

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class HomeViewModel constructor(
    private val repository: YouTubeRepository,
    private val recentlyPlayedRepository: RecentlyPlayedRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())

    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            recentlyPlayedRepository.recentlyPlayed
                .map { entities -> entities.map { it.toRecentlyPlayedItem() } }
                .collect { items ->
                    _uiState.update { it.copy(recentlyPlayed = items) }
                }
        }
    }

    fun onHomeAction(action: HomeAction) {
        when(action) {
            is HomeAction.OnRecentPlayedClicked -> {

            }
        }
    }

}


data class HomeUiState(
    val isLoading: Boolean = false,
    val recentlyPlayed: List<RecentlyPlayedItem> = emptyList()
)

sealed interface HomeAction {
    data class OnRecentPlayedClicked(val recentlyPlayedItem: RecentlyPlayedItem): HomeAction
}
