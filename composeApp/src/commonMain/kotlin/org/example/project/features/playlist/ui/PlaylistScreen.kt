package org.example.project.features.playlist.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import org.example.project.core.model.Playlist
import org.example.project.core.model.PlaylistSong
import org.example.project.core.model.Song
import org.example.project.features.songMenu.ui.SongMenuAction
import org.example.project.features.songMenu.ui.rememberSongMenuController
import org.example.project.ui.component.CoverImage
import org.example.project.ui.component.SongCollectionHeader
import org.example.project.ui.component.SongCollectionScaffold
import org.example.project.ui.theme.AppPreview
import org.example.project.ui.theme.DevicePreviews
import org.example.project.ui.theme.Dimens

private val playlistSongMenuActions = listOf(
    SongMenuAction.AddToQueue,
    SongMenuAction.AddToPlaylist,
    SongMenuAction.GoToArtist,
    SongMenuAction.GoToAlbum,
    SongMenuAction.RemoveFromPlaylist
)

@Composable
fun PlaylistScreen(
    state: PlaylistUiState,
    onBackPressed: () -> Unit,
    onAction: (PlaylistAction) -> Unit
) {
    val songMenu = rememberSongMenuController()
    val playlist = state.playlist

    SongCollectionScaffold(
        title = playlist?.name ?: "",
        items = playlist?.songs ?: emptyList(),
        songOf = { it.song },
        itemKey = { it.id },
        onBackPressed = onBackPressed,
        onSongClicked = { playlistSong, index ->
            onAction(PlaylistAction.OnPlaylistSongPressed(playlistSong.song, index))
        },
        onSongMenuClicked = { playlistSong ->
            songMenu.show(playlistSong.song, playlistSongMenuActions, playlistSongId = playlistSong.id)
        },
        isLoading = state.isLoading,
        emptyMessage = if (playlist == null) "Playlist not found" else "No songs in playlist",
        currentlyPlayingSongId = state.currentlyPlayingSongId,
        isContextActive = state.isPlaylistActive,
        isPlaying = state.isPlaying,
        topBarActions = {
            IconButton(onClick = { onAction(PlaylistAction.OnSearchPressed) }) {
                Icon(Icons.Default.Search, contentDescription = "Search")
            }
        },
        header = playlist?.let {
            {
                SongCollectionHeader(
                    songCount = it.songs.count(),
                    isPlaying = state.isPlaying && state.isPlaylistActive,
                    onShufflePressed = { onAction(PlaylistAction.OnShuffledPressed) },
                    onMenuPressed = { onAction(PlaylistAction.OnMenuPressed) },
                    onPlayPressed = { onAction(PlaylistAction.OnPlayPressed) },
                    title = it.name,
                    artwork = {
                        CoverImage(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = Dimens.Size.playlistCoverInset),
                            data = it.thumbnailUrl
                        )
                    }
                )
            }
        }
    )
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
