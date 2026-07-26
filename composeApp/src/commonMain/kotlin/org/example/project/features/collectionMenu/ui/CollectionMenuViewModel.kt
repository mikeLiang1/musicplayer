package org.example.project.features.collectionMenu.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.example.project.core.manager.QueueManager
import org.example.project.features.playlist.repository.PlaylistRepository

/**
 * Owns the collection-level menu and everything it opens (rename sheet, delete confirmation),
 * so screens don't carry that state themselves — the same split `SongMenuViewModel` makes for
 * the per-song menu. Reached through [rememberCollectionMenuController].
 */
class CollectionMenuViewModel(
    private val playlistRepository: PlaylistRepository,
    private val queueManager: QueueManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(CollectionMenuState())
    val uiState = _uiState.asStateFlow()

    private val _effect = MutableSharedFlow<CollectionMenuEffect>()
    val effect: SharedFlow<CollectionMenuEffect> = _effect.asSharedFlow()

    fun onMenuClicked(target: CollectionMenuTarget) {
        _uiState.value = CollectionMenuState(target = target, isMenuVisible = true)
    }

    fun onCloseMenuSheet() {
        _uiState.update { it.copy(isMenuVisible = false) }
    }

    /** The menu sheet closes itself on selection, so each branch only opens whatever comes next. */
    fun handleAction(action: CollectionMenuAction) {
        val target = _uiState.value.target ?: return
        when (action) {
            CollectionMenuAction.AddToQueue -> {
                queueManager.addToManualQueue(target.songs)
                _uiState.update { it.copy(isMenuVisible = false) }
                // Nothing on screen changes when songs go to the queue, so say so.
                val label = if (target.songs.size == 1) "1 song" else "${target.songs.size} songs"
                viewModelScope.launch {
                    _effect.emit(CollectionMenuEffect.ShowMessage("Added $label to queue"))
                }
            }

            CollectionMenuAction.Rename -> _uiState.update {
                it.copy(isMenuVisible = false, renameName = target.title)
            }

            CollectionMenuAction.Delete -> _uiState.update {
                it.copy(isMenuVisible = false, isDeleteConfirmVisible = true)
            }
        }
    }

    fun onRenameNameChanged(name: String) {
        _uiState.update { it.copy(renameName = name) }
    }

    fun onCloseRenameSheet() {
        _uiState.update { it.copy(renameName = null, isSaving = false) }
    }

    fun onConfirmRename() {
        val state = _uiState.value
        val target = state.target as? CollectionMenuTarget.UserPlaylist ?: return
        val name = state.renameName?.trim().orEmpty()
        // Guards a blank name and a double-tap on Save, which would otherwise fire two writes
        // before the first one closes the sheet.
        if (name.isEmpty() || state.isSaving) return
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            playlistRepository.renamePlaylist(target.playlistId, name)
            _uiState.value = CollectionMenuState()
        }
    }

    fun onCloseDeleteConfirm() {
        _uiState.update { it.copy(isDeleteConfirmVisible = false) }
    }

    fun onConfirmDelete() {
        val state = _uiState.value
        val target = state.target as? CollectionMenuTarget.UserPlaylist ?: return
        if (state.isSaving) return
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            // Songs go with it — playlist_songs is FK-cascaded on the playlist row. Anything
            // already playing keeps playing; the queue holds copies, not a view of the table.
            playlistRepository.deletePlaylist(target.playlistId)
            _uiState.value = CollectionMenuState()
            _effect.emit(CollectionMenuEffect.Deleted(target.playlistId))
        }
    }
}

data class CollectionMenuState(
    val target: CollectionMenuTarget? = null,
    val isMenuVisible: Boolean = false,
    /** Holds the in-progress text, so non-null doubles as "the rename sheet is open". */
    val renameName: String? = null,
    val isDeleteConfirmVisible: Boolean = false,
    val isSaving: Boolean = false
) {
    val actions: List<CollectionMenuAction> get() = target?.actions.orEmpty()
}

sealed interface CollectionMenuEffect {
    /**
     * The playlist was deleted. A screen showing that playlist should leave; a screen merely
     * listing it (Library) can ignore this — Room already drops the row.
     */
    data class Deleted(val playlistId: String) : CollectionMenuEffect
    data class ShowMessage(val message: String) : CollectionMenuEffect
}
