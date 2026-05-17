package org.example.project.features.home.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.example.project.core.database.entity.RecentlyPlayedType
import org.example.project.core.database.mapper.toRecentlyPlayedItem
import org.example.project.core.model.RecentlyPlayedItem
import org.example.project.core.repository.RecentlyPlayedRepository
import org.example.project.core.usecase.PlaySongUseCase
import org.example.project.features.playlist.repository.PlaylistRepository

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class HomeViewModel(
    private val recentlyPlayedRepository: RecentlyPlayedRepository,
    private val playSongUseCase: PlaySongUseCase,
    private val playlistRepository: PlaylistRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState = _uiState.asStateFlow()

    private val _effect = MutableSharedFlow<HomeEffect>()
    val effect: SharedFlow<HomeEffect> = _effect.asSharedFlow()

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
        when (action) {
            is HomeAction.OnRecentPlayedClicked -> {
                when (action.recentlyPlayedItem.contentType) {
                    RecentlyPlayedType.SONG -> {
                        viewModelScope.launch {
                            playSongUseCase(action.recentlyPlayedItem.contentId)
                        }
                    }

                    RecentlyPlayedType.PLAYLIST -> {
                        viewModelScope.launch {
                            _effect.emit(HomeEffect.NavigateToPlaylist(action.recentlyPlayedItem.contentId))
                        }
                    }
                }
            }
        }
    }

}


data class HomeUiState(
    val isLoading: Boolean = false,
    val recentlyPlayed: List<RecentlyPlayedItem> = emptyList()
)

sealed interface HomeAction {
    data class OnRecentPlayedClicked(val recentlyPlayedItem: RecentlyPlayedItem) : HomeAction
}

sealed interface HomeEffect {
    data class NavigateToPlaylist(val playlistId: String) : HomeEffect
}
