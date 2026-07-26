package org.example.project.features.likedSongs.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.example.project.core.model.Song
import org.example.project.features.songMenu.ui.SongMenuAction
import org.example.project.features.songMenu.ui.rememberSongMenuController
import org.example.project.ui.component.SongItem
import org.example.project.ui.component.SongItemState
import org.example.project.ui.theme.AppPreview
import org.example.project.ui.theme.DevicePreviews
import org.example.project.ui.theme.Dimens
import org.example.project.ui.theme.appColors

@Composable
fun LikedSongsScreen(
    state: LikedSongsUiState,
    onBackPressed: () -> Unit,
    onAction: (LikedSongsAction) -> Unit
) {
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            Box(
                modifier = Modifier
                    .background(color = appColors.backgroundElevated)
                    .fillMaxWidth()
                    .padding(vertical = Dimens.spaceS),
                contentAlignment = Alignment.CenterStart
            ) {
                IconButton(onClick = onBackPressed) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "back")
                }
                Text(
                    text = "Liked Songs",
                    color = appColors.textPrimary,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(start = Dimens.Size.iconChip + Dimens.spaceM)
                )
            }
        }
    ) { innerPadding ->
        if (state.isLoading) {
            CircularProgressIndicator(modifier = Modifier.padding(innerPadding))
        } else if (state.songs.isEmpty()) {
            Text(
                "No liked songs yet",
                color = appColors.textMuted,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(innerPadding).padding(Dimens.spaceL)
            )
        } else {
            val songMenu = rememberSongMenuController()
            val menuActions = listOf(
                SongMenuAction.AddToQueue,
                SongMenuAction.AddToPlaylist,
                SongMenuAction.GoToArtist,
                SongMenuAction.GoToAlbum
            )
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                itemsIndexed(state.songs, key = { _, song -> song.uniqueId }) { index, song ->
                    SongItem(
                        song = song,
                        onClick = { onAction(LikedSongsAction.OnSongClicked(song, index)) },
                        onMenuClicked = { songMenu.show(song, menuActions) },
                        state = if (state.currentlyPlayingSongId == song.uniqueId && state.isContextActive)
                            SongItemState.Current(state.isPlaying) else SongItemState.Default
                    )
                }
            }
        }
    }
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
