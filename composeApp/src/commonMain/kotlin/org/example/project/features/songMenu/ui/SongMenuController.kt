package org.example.project.features.songMenu.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.SharedFlow
import org.example.project.core.model.Song
import org.example.project.ui.component.MenuBottomSheet
import org.koin.compose.viewmodel.koinViewModel


class SongMenuController(
    private val viewModel: SongMenuViewModel
) {
    /** One-shot effects from menu rows the song menu can't service itself (see [SongMenuEffect]). */
    val effect: SharedFlow<SongMenuEffect> = viewModel.effect

    fun show(song: Song, options: List<SongMenuAction>, playlistSongId: String? = null) {
        // Direct call to the "Source of Truth"
        viewModel.onMenuClicked(song, options, playlistSongId)
    }

    /** Skips the song menu and opens the add-to-playlist sheet straight away. */
    fun showAddToPlaylist(song: Song) {
        viewModel.onAddToPlaylistClicked(song)
    }
}


@Composable
fun rememberSongMenuController(): SongMenuController {
    val viewModel: SongMenuViewModel = koinViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val isSelectedSongLiked by viewModel.isSelectedSongLiked.collectAsStateWithLifecycle()
    val likedSongCount by viewModel.likedSongCount.collectAsStateWithLifecycle()
    val checkedPlaylistIds by viewModel.selectedSongPlaylistIds.collectAsStateWithLifecycle()

    val controller = remember {
        SongMenuController(viewModel = viewModel)
    }

    MenuBottomSheet(
        isVisible = uiState.isMenuSheetVisible,
        actions = uiState.menuActions,
        onActionSelected = viewModel::handleAction,
        onDismissRequest = { viewModel.onCloseMenuSheet() }
    )

    AddToPlaylistBottomSheet(
        isBottomSheetVisible = uiState.isPlaylistSheetVisible,
        onCloseBottomSheet = viewModel::onClosePlaylistSheet,
        playlists = playlists,
        isSongLiked = isSelectedSongLiked,
        likedSongCount = likedSongCount,
        checkedPlaylistIds = checkedPlaylistIds,
        onLikedSongsClicked = viewModel::toggleLikeForSelectedSong,
        onPlaylistClicked = viewModel::togglePlaylistForSelectedSong
    )

    return controller

}
