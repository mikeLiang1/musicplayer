package org.example.project.features.playlist.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.example.project.core.model.Playlist
import org.example.project.core.model.PlaylistSong
import org.example.project.core.model.Song
import org.example.project.features.songMenu.ui.SongMenuAction
import org.example.project.features.songMenu.ui.rememberSongMenuController
import org.example.project.ui.component.CoverImage
import org.example.project.ui.component.PlayPauseButton
import org.example.project.ui.component.SongItem
import org.example.project.ui.component.SongItemState
import org.example.project.ui.theme.AppPreview
import org.example.project.ui.theme.DevicePreviews
import org.example.project.ui.theme.appColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistScreen(
    state: PlaylistUiState,
    onBackPressed: () -> Unit,
    onAction: (PlaylistAction) -> Unit
) {
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .background(color = appColors.backgroundElevated)
                    .fillMaxWidth()
            ) {
                IconButton(
                    onClick = onBackPressed
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "back")
                }

                Text(text = state.playlist?.name ?: "", color = appColors.textPrimary)

                IconButton(onClick = { onAction(PlaylistAction.OnSearchPressed) }) {
                    Icon(Icons.Default.Search, contentDescription = "Search")
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (state.isLoading) {
                CircularProgressIndicator()
            } else if (state.playlist == null) {
                Text("Playlist not found")
            } else {

                val songMenu = rememberSongMenuController(
                    listOf(
                        SongMenuAction.AddToQueue,
                        SongMenuAction.AddToPlaylist,
                        SongMenuAction.GoToArtist,
                        SongMenuAction.GoToAlbum,
                        SongMenuAction.RemoveFromPlaylist
                    )
                )
                LazyColumn(
                    contentPadding = PaddingValues(vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    item {
                        PlaylistHeader(playlist = state.playlist, onAction = onAction, state.isPlaying)
                    }
                    item {
                        HorizontalDivider(color = appColors.divider, modifier = Modifier.padding(vertical = 12.dp))
                    }
                    if (state.playlist.songs.isEmpty()) {
                        item {
                            Text("No songs in playlist")
                        }
                    } else {
                        itemsIndexed(state.playlist.songs, key = { _, item -> item.id }) { index, song ->
                            SongItem(
                                song = song.song,
                                onClick = {
                                    onAction(PlaylistAction.OnPlaylistSongPressed(song.song, index))
                                },
                                onMenuClicked = {
                                    songMenu.show(song.song, playlistSongId = song.id)
                                },
                                state = if (state.currentlyPlayingSongId == song.song.uniqueId && state.isPlaylistActive)
                                    SongItemState.Current(state.isPlaying) else SongItemState.Default
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaylistHeader(playlist: Playlist, onAction: (PlaylistAction) -> Unit, isPlaying: Boolean) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        CoverImage(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 80.dp), data = playlist.thumbnailUrl
        )

        Text(
            playlist.name,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = 12.dp)
        )

        Row(
            modifier = Modifier
                .padding(top = 8.dp)
                .padding(horizontal = 12.dp)
        ) {
            Text(
                "${playlist.songs.count()} songs", style = MaterialTheme.typography.bodySmall,
                color = appColors.textMuted
            )
            // TODO: Total duration ?

        }
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {

            Row {
                IconButton(onClick = { onAction(PlaylistAction.OnShuffledPressed) }) {

                    Icon(Icons.Default.Shuffle, contentDescription = "shuffle")
                }

                Spacer(modifier = Modifier.width(12.dp))
                IconButton(onClick = { onAction(PlaylistAction.OnMenuPressed) }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "Menu")
                }
            }

            PlayPauseButton(
                onPressed = { onAction(PlaylistAction.OnPlayPressed) },
                isPlaying = isPlaying,
                modifier = Modifier.padding(end = 12.dp)
            )
        }
    }
}

@DevicePreviews
@Composable
private fun PlaylistPreview() {
    AppPreview {
        PlaylistScreen(
            state = PlaylistUiState(
                playlist = Playlist(
                    name = "Title", id = "", thumbnailUrl = "", songs = listOf(
                        PlaylistSong(
                            song = Song(
                                url = "item.url",
                                title = "Currently Playing Song",
                                artist = "Artist",
                                thumbnailUrl = "item.thumbnails.firstOrNull()?.url",
                                duration = 3000L
                            ), position = 0, id = ""
                        )
                    )
                )
            ),
            onBackPressed = {},
            onAction = {}
        )
    }
}

@Preview
@Composable
private fun NoSongPreview() {
    AppPreview {
        PlaylistScreen(
            state = PlaylistUiState(
                playlist = Playlist(name = "Title", id = "", thumbnailUrl = "", songs = listOfNotNull())
            ),
            onBackPressed = {},
            onAction = {}
        )
    }
}
