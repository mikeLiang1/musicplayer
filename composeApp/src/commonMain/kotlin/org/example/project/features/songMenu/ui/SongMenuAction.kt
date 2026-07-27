package org.example.project.features.songMenu.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.PlaylistRemove
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Queue
import androidx.compose.material.icons.rounded.Timer
import org.example.project.ui.component.MenuAccent
import org.example.project.ui.component.MenuAction

/**
 * Rows of the per-song context menu. Each screen passes the subset that makes sense there
 * (see `playlistSongMenuActions` in PlaylistScreen); `SongMenuViewModel` prepends Like/Unlike.
 */
sealed class SongMenuAction : MenuAction {

    data object Like : SongMenuAction() {
        override val label = "Like"
        override val icon = Icons.Outlined.FavoriteBorder
        override val accent = MenuAccent.Like
    }

    data object Unlike : SongMenuAction() {
        override val label = "Unlike"
        override val icon = Icons.Rounded.Favorite
        override val accent = MenuAccent.Like
    }

    data object GoToArtist : SongMenuAction() {
        override val label = "Go to artist"
        override val icon = Icons.Rounded.Person
    }

    data object GoToAlbum : SongMenuAction() {
        override val label = "Go to album"
        override val icon = Icons.Rounded.Album
    }

    data object AddToPlaylist : SongMenuAction() {
        override val label = "Add to playlist"
        override val icon = Icons.AutoMirrored.Rounded.PlaylistAdd
    }

    data object RemoveFromPlaylist : SongMenuAction() {
        override val label = "Remove from this playlist"
        override val icon = Icons.Outlined.PlaylistRemove
    }

    data object AddToQueue : SongMenuAction() {
        override val label = "Add to queue"
        override val icon = Icons.Rounded.Queue
    }

    data object RemoveFromQueue : SongMenuAction() {
        override val label = "Remove from queue"
        override val icon = Icons.Rounded.DeleteOutline
        override val accent = MenuAccent.Destructive
    }

    /**
     * Player-only row: playback-scoped rather than song-scoped, so it is passed explicitly by
     * the full-screen player and deliberately kept out of [all]. Selecting it emits
     * [SongMenuEffect.OpenSleepTimer] instead of doing work in the song menu itself.
     */
    data object SleepTimer : SongMenuAction() {
        override val label = "Sleep timer"
        override val icon = Icons.Rounded.Timer
    }

    companion object {
        val all = listOf(
            Like,
            AddToQueue,
            AddToPlaylist,
            GoToArtist,
            GoToAlbum,
            RemoveFromQueue
        )
    }
}
