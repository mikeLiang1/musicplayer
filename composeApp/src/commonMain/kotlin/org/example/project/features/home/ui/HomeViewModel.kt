package org.example.project.features.home.ui

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yushosei.newpipe.util.ExtractorHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.example.project.core.model.Song
import org.example.project.core.repository.YouTubeRepository

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class HomeViewModel constructor(
    private val repository: YouTubeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState = _uiState.asStateFlow()

    init {
        // 2. The Search Pipeline
        // We launch a coroutine that listens to changes in _uiState
        viewModelScope.launch {
            _uiState
                // A. EXTRACT: We only care if the 'searchQuery' part changes
                .map { it.searchQuery }

                // B. GUARD: Crucial! Only proceed if the text is actually different
                // from the last time we checked.
                .distinctUntilChanged()

                // C. DEBOUNCE: Wait 500ms
                .debounce(500L)

                // D. TRANSFORM: Execute the search
                .flatMapLatest { query ->
                    if (query.isBlank()) {
                        flowOf(emptyList())
                    } else {
                        flow {
                            // Optional: Set loading state here if you want
                            // but be careful not to trigger infinite loops
                            emit(repository.getSearchSuggestion(query))
                        }
                            .catch { emit(emptyList()) }
                    }
                }

                // E. COLLECT: Update the state with the results
                .collect { suggestions ->
                    _uiState.update { currentState ->
                        currentState.copy(
                            suggestions = suggestions,
                            isLoading = false
                        )
                    }
                }
        }
    }

    fun searchSongs() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(isLoading = true)
            }
            val songs = repository.searchSongs(_uiState.value.searchQuery)
            _uiState.update {
                it.copy(isLoading = false, songList = songs)
            }
        }
    }


    fun onQueryChanged(newText: String) {
        _uiState.update {
            it.copy(searchQuery = newText)
        }
    }
}


data class HomeUiState(
    val isLoading: Boolean = false,
    val searchQuery: String = "",
    val songList: List<Song> = listOf(),
    val suggestions: List<String> = listOf()
)

