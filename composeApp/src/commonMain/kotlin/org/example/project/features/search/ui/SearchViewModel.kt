package org.example.project.features.search.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.example.project.core.model.Song
import org.example.project.core.repository.InnerTubeRepository
import org.example.project.core.repository.NewPipeRepository
import org.example.project.core.repository.RecentlyPlayedRepository
import org.example.project.core.usecase.PlaySongUseCase

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class SearchViewModel(
    private val newPipeRepository: NewPipeRepository,
    private val playSongUseCase: PlaySongUseCase,
    private val recentlyPlayedRepository: RecentlyPlayedRepository,
    private val innerTubeRepository: InnerTubeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState = _uiState.asStateFlow()

    private val _effect = MutableSharedFlow<SearchEffect>()
    val effect: SharedFlow<SearchEffect> = _effect.asSharedFlow()


    private val searchQuery = MutableStateFlow("")

    init {
        viewModelScope.launch {
            searchQuery
                .debounce(300L)
                .distinctUntilChanged()
                .flatMapLatest { query ->
                    if (query.isBlank()) {
                        flowOf(Result.success(emptyList()))
                    } else {
                        flow { emit(Result.success(newPipeRepository.getSearchSuggestions(query))) }
                            .catch { emit(Result.failure(it)) }
                            .onStart { _uiState.update { it.copy(isLoading = true) } }
                    }
                }
                .collect { result ->
                    _uiState.update {
                        it.copy(
                            suggestions = result.getOrDefault(emptyList()),
                            isLoading = false
                        )
                    }
                }
        }
    }

    fun handleAction(searchAction: SearchAction) {
        when (searchAction) {
            SearchAction.OnBackPressed -> {
                _uiState.update {
                    it.copy(songList = listOf(), onSearchScreen = true)
                }
            }

            SearchAction.OnTextCleared -> {
                _uiState.update {
                    it.copy(searchQuery = "", onSearchScreen = true, suggestions = listOf())
                }
            }

            is SearchAction.OnQueryChanged -> {
                searchQuery.value = searchAction.query
                _uiState.update {
                    it.copy(searchQuery = searchAction.query, onSearchScreen = true)
                }
            }

            is SearchAction.OnSongClicked -> {
                viewModelScope.launch {
                    val song = searchAction.song
                    playSongUseCase(song.url)
                    recentlyPlayedRepository.recordSong(song)
                }
            }

            is SearchAction.OnSuggestionClicked -> {
                _uiState.update {
                    it.copy(searchQuery = searchAction.suggestion, onSearchScreen = false, isLoading = true)
                }
                viewModelScope.launch {
                    // TODO: Try catch
                    val searchResult = innerTubeRepository.searchSongs(searchAction.suggestion)
                    _uiState.update {
                        it.copy(songList = searchResult.songs, isLoading = false, searchToken = searchResult.continuationToken)
                    }
                    Log.d("Logging", searchResult.albums.toString())
                    Log.d("Logging", searchResult.artists.toString())
                }

            }

            SearchAction.SearchMoreSongs -> {
                if (uiState.value.isLoadingMore || uiState.value.onSearchScreen || uiState.value.isLoading) return
                val searchToken = _uiState.value.searchToken
                if (searchToken == null) return
                viewModelScope.launch {
                    _uiState.update {
                        it.copy(isLoadingMore = true)
                    }
                    val searchResult = innerTubeRepository.searchMoreSongs(searchToken)
                    _uiState.update {
                        it.copy(
                            songList = _uiState.value.songList + searchResult.songs,
                            isLoadingMore = false,
                            searchToken = searchResult.continuationToken
                        )
                    }
                }
            }
        }
    }

}

sealed interface SearchEffect {
    data object NavigateToResult : SearchEffect
}

sealed interface SearchAction {
    data object OnBackPressed : SearchAction
    data object OnTextCleared : SearchAction
    data object SearchMoreSongs : SearchAction
    data class OnQueryChanged(val query: String) : SearchAction
    data class OnSongClicked(val song: Song) : SearchAction
    data class OnSuggestionClicked(val suggestion: String) : SearchAction
}


data class SearchUiState(
    val searchQuery: String = "",
    val suggestions: List<String> = listOf(),
    val songList: List<Song> = listOf(),
    val onSearchScreen: Boolean = true,
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val searchToken: String? = null
)
