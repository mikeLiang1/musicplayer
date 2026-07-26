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

    val controller = remember {
        SongMenuController(viewModel = viewModel)
    }

    SongMenuBottomSheet(
        isMenuBottomSheetVisible = uiState.isMenuSheetVisible,
        onCloseBottomSheet = {
            viewModel.onCloseMenuSheet()
        },
        handleBottomSheetAction = viewModel::handleAction,
        songMenuActions = uiState.menuActions
    )

    AddToPlaylistBottomSheet(
        isBottomSheetVisible = uiState.isPlaylistSheetVisible,
        onCloseBottomSheet = viewModel::onClosePlaylistSheet,
        playlists = playlists,
        isSongLiked = isSelectedSongLiked,
        likedSongCount = likedSongCount,
        onLikedSongsClicked = viewModel::toggleLikeForSelectedSong,
        onPlaylistClicked = viewModel::addSongToSelectedPlaylist
    )

    return controller

}
