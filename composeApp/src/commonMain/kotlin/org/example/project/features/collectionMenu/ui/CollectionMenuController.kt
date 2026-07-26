package org.example.project.features.collectionMenu.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.example.project.core.model.Playlist
import org.example.project.core.model.Song
import org.example.project.ui.component.ConfirmDialog
import org.example.project.ui.component.MenuBottomSheet
import org.example.project.ui.component.PlaylistNameBottomSheet
import org.koin.compose.viewmodel.koinViewModel

class CollectionMenuController(
    private val viewModel: CollectionMenuViewModel
) {
    /** Full menu for a user playlist: queue, rename, delete. */
    fun show(playlist: Playlist) {
        viewModel.onMenuClicked(
            CollectionMenuTarget.UserPlaylist(
                playlistId = playlist.id,
                title = playlist.name,
                songs = playlist.songs.map { it.song }
            )
        )
    }

    /** Queue-only menu for a system collection such as Liked Songs. */
    fun show(title: String, songs: List<Song>) {
        viewModel.onMenuClicked(CollectionMenuTarget.SystemCollection(title, songs))
    }
}

/**
 * Call at the top of a screen that has a collection-level ⋮ — it composes the menu sheet, the
 * rename sheet and the delete confirmation itself, and returns the handle used to open them.
 * Mirrors `rememberSongMenuController`; don't call it twice in nested composables or the sheets
 * compose twice.
 *
 * [onDeleted] fires after the playlist is gone — a screen *showing* that playlist passes its own
 * back action; a screen merely listing it can leave the default. [onMessage] receives short
 * confirmations for actions with no visible effect, and is wired to the dashboard snackbar.
 */
@Composable
fun rememberCollectionMenuController(
    onDeleted: () -> Unit = {},
    onMessage: (String) -> Unit = {}
): CollectionMenuController {
    val viewModel: CollectionMenuViewModel = koinViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val controller = remember { CollectionMenuController(viewModel) }

    // The callbacks come from the calling screen and can change between recompositions, while
    // the collector below is started once — read them through the latest snapshot.
    val currentOnDeleted by rememberUpdatedState(onDeleted)
    val currentOnMessage by rememberUpdatedState(onMessage)
    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is CollectionMenuEffect.Deleted -> currentOnDeleted()
                is CollectionMenuEffect.ShowMessage -> currentOnMessage(effect.message)
            }
        }
    }

    MenuBottomSheet(
        isVisible = uiState.isMenuVisible,
        actions = uiState.actions,
        onActionSelected = viewModel::handleAction,
        onDismissRequest = viewModel::onCloseMenuSheet
    )

    PlaylistNameBottomSheet(
        isVisible = uiState.renameName != null,
        name = uiState.renameName.orEmpty(),
        isSaving = uiState.isSaving,
        onNameChange = viewModel::onRenameNameChanged,
        onConfirm = viewModel::onConfirmRename,
        onDismissRequest = viewModel::onCloseRenameSheet,
        title = "Rename playlist",
        confirmLabel = "Save"
    )

    ConfirmDialog(
        isVisible = uiState.isDeleteConfirmVisible,
        title = "Delete playlist?",
        message = deleteMessage(uiState.target?.title, uiState.target?.songs?.size ?: 0),
        confirmLabel = "Delete",
        isDestructive = true,
        onConfirm = viewModel::onConfirmDelete,
        onDismissRequest = viewModel::onCloseDeleteConfirm
    )

    return controller
}

/** Names what is about to be lost — deleting takes the playlist's songs with it. */
private fun deleteMessage(name: String?, songCount: Int): String {
    val subject = if (name.isNullOrBlank()) "This playlist" else "\"$name\""
    val songs = when (songCount) {
        0 -> ""
        1 -> " and its 1 song"
        else -> " and its $songCount songs"
    }
    return "$subject$songs will be removed. This can't be undone."
}
