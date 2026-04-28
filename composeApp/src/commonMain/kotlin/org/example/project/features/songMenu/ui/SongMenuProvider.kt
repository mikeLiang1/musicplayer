package org.example.project.features.songMenu.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel


@Composable
fun SongMenuProvider(
    selectedMenuTarget: SelectedMenuTarget?,
    onTargetConsumed: () -> Unit,
    songMenuOptions: List<SongMenuAction>
) {
    val viewModel: SongMenuViewModel = koinViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()

    LaunchedEffect(selectedMenuTarget) {
        selectedMenuTarget?.let {
            viewModel.onMenuClicked(song = it.song, it.playlistSongId)
            onTargetConsumed()
        }
    }

    SongMenuBottomSheet(
        isMenuBottomSheetVisible = uiState.isMenuSheetVisible,
        onCloseBottomSheet = {
            viewModel.onCloseMenuSheet()
        },
        handleBottomSheetAction = viewModel::handleAction,
        songMenuActions = songMenuOptions
    )

    AddToPlaylistBottomSheet(
        isBottomSheetVisible = uiState.isPlaylistSheetVisible,
        onCloseBottomSheet = viewModel::onClosePlaylistSheet,
        playlists = playlists,
        onPlaylistClicked = viewModel::addSongToSelectedPlaylist
    )
}
