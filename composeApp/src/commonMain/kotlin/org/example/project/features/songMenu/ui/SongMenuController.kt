package org.example.project.features.songMenu.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.example.project.core.model.Song
import org.koin.compose.viewmodel.koinViewModel


class SongMenuController(
    private val viewModel: SongMenuViewModel
) {
    fun show(song: Song, playlistSongId: String? = null) {
        // Direct call to the "Source of Truth"
        viewModel.onMenuClicked(song, playlistSongId)
    }
}


@Composable
fun rememberSongMenuController(
    options: List<SongMenuAction>
): SongMenuController {
    val viewModel: SongMenuViewModel = koinViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()

    val controller = remember {
        SongMenuController(viewModel = viewModel)
    }

    SongMenuBottomSheet(
        isMenuBottomSheetVisible = uiState.isMenuSheetVisible,
        onCloseBottomSheet = {
            viewModel.onCloseMenuSheet()
        },
        handleBottomSheetAction = viewModel::handleAction,
        songMenuActions = options
    )

    AddToPlaylistBottomSheet(
        isBottomSheetVisible = uiState.isPlaylistSheetVisible,
        onCloseBottomSheet = viewModel::onClosePlaylistSheet,
        playlists = playlists,
        onPlaylistClicked = viewModel::addSongToSelectedPlaylist
    )

    return controller

}
