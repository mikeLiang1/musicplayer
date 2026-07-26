package org.example.project.features.likedSongs.ui

import androidx.compose.runtime.Composable
import org.example.project.core.model.Song
import org.example.project.features.songMenu.ui.SongMenuAction
import org.example.project.features.songMenu.ui.rememberSongMenuController
import org.example.project.ui.component.SongCollectionHeader
import org.example.project.ui.component.SongCollectionScaffold
import org.example.project.ui.theme.AppPreview
import org.example.project.ui.theme.DevicePreviews

private val likedSongMenuActions = listOf(
    SongMenuAction.AddToQueue,
    SongMenuAction.AddToPlaylist,
    SongMenuAction.GoToArtist,
    SongMenuAction.GoToAlbum
)

@Composable
fun LikedSongsScreen(
    state: LikedSongsUiState,
    onBackPressed: () -> Unit,
    onAction: (LikedSongsAction) -> Unit
) {
    val songMenu = rememberSongMenuController()

    SongCollectionScaffold(
        title = "Liked Songs",
        items = state.songs,
        songOf = { it },
        itemKey = { it.uniqueId },
        onBackPressed = onBackPressed,
        onSongClicked = { song, index -> onAction(LikedSongsAction.OnSongClicked(song, index)) },
        onSongMenuClicked = { song -> songMenu.show(song, likedSongMenuActions) },
        isLoading = state.isLoading,
        emptyMessage = "No liked songs yet",
        currentlyPlayingSongId = state.currentlyPlayingSongId,
        isContextActive = state.isContextActive,
        isPlaying = state.isPlaying,
        // No artwork and no title — liked songs has no cover, and the top bar already names it.
        header = {
            SongCollectionHeader(
                songCount = state.songs.count(),
                isPlaying = state.isPlaying && state.isContextActive,
                onShufflePressed = { onAction(LikedSongsAction.OnShufflePressed) },
                onMenuPressed = { onAction(LikedSongsAction.OnMenuPressed) },
                onPlayPressed = { onAction(LikedSongsAction.OnPlayPressed) }
            )
        }
    )
}

@DevicePreviews
@Composable
private fun LikedSongsPreview() {
    AppPreview {
        LikedSongsScreen(
            state = LikedSongsUiState(
                songs = listOf(
                    Song(
                        url = "item.url",
                        title = "Liked Song",
                        artist = "Artist",
                        thumbnailUrl = null,
                        duration = 3000L,
                        isLiked = true
                    )
                )
            ),
            onBackPressed = {},
            onAction = {}
        )
    }
}

@DevicePreviews
@Composable
private fun LikedSongsEmptyPreview() {
    AppPreview {
        LikedSongsScreen(state = LikedSongsUiState(), onBackPressed = {}, onAction = {})
    }
}
