package org.example.project.features.search.ui

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
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.example.project.core.manager.MusicPlayerManager
import org.example.project.core.model.Song
import org.example.project.core.repository.YouTubeRepository

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class SearchViewModel constructor(
    private val repository: YouTubeRepository,
    private val musicPlayerManager: MusicPlayerManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState = _uiState.asStateFlow()

    private val _effect = MutableSharedFlow<SearchEffect>()
    val effect: SharedFlow<SearchEffect> = _effect.asSharedFlow()


    private val searchQuery = MutableStateFlow("")

    init {
        viewModelScope.launch {
            searchQuery
                .debounce(500L)
                .flatMapLatest { query ->
                    if (query.isBlank()) {
                        flowOf(emptyList())
                    } else {
                        flow {
                            emit(repository.getSearchSuggestion(query))
                        }.catch { emit(emptyList()) }
                    }
                }
                .collect { suggestions ->
                    _uiState.update { it.copy(suggestions = suggestions) }
                }
        }
    }

    fun onSuggestionClicked(suggestion: String) {
        _uiState.update {
            it.copy(searchQuery = suggestion, onSearchScreen = false, isLoading = true)
        }
        viewModelScope.launch {
            // TODO: Try catch
            val songList = repository.searchSongs(suggestion)
            _uiState.update {
                it.copy(songList = songList, isLoading = false)
            }
        }
    }

    fun searchMoreSongs() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(isLoadingMore = true)
            }
            val songList = repository.searchMoreSongs(_uiState.value.searchQuery)
            _uiState.update {
                it.copy(songList = _uiState.value.songList + songList, isLoadingMore = false)
            }
        }
    }

    fun onBackPressed() {
        _uiState.update {
            it.copy(songList = listOf(), onSearchScreen = true)
        }
    }


    fun onQueryChanged(query: String) {
        searchQuery.value = query
        _uiState.update {
            it.copy(searchQuery = query)
        }
    }

    fun onSongClicked(song: Song) {
        viewModelScope.launch {
            musicPlayerManager.prepare(song = song, autoPlay = true)
        }
        viewModelScope.launch {
            val songList =  repository.getPlaylist(song.url)
        }
    }
}

sealed interface SearchEffect {
    data object NavigateToResult : SearchEffect
}


data class SearchUiState(
    val searchQuery: String = "",
    val suggestions: List<String> = listOf(),
    val songList: List<Song> = listOf(),
    val onSearchScreen: Boolean = true,
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false
)
