package org.example.project.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.example.project.ui.theme.AppPreview
import org.example.project.ui.theme.DevicePreviews
import org.example.project.ui.theme.Dimens
import org.example.project.ui.theme.appColors

/**
 * Header for a song-collection screen: song count, shuffle + menu icons, and the play FAB.
 * Designed to sit in [SongCollectionScaffold]'s `header` slot.
 *
 * [artwork] and [title] are optional slots. PlaylistScreen supplies both (a full-width
 * `CoverImage` and the playlist name); LikedSongsScreen supplies neither — it has no
 * artwork, and the scaffold's top bar already shows its name, so the header is just the
 * count and the controls.
 *
 * Pass `isPlaying = state.isPlaying && isContextActive` so the FAB doesn't render as
 * "pause" while some other collection is what's actually playing.
 */
@Composable
fun SongCollectionHeader(
    songCount: Int,
    isPlaying: Boolean,
    onShufflePressed: () -> Unit,
    onMenuPressed: () -> Unit,
    onPlayPressed: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    artwork: (@Composable () -> Unit)? = null
) {
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        artwork?.invoke()

        if (title != null) {
            Text(
                title,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = Dimens.spaceM)
            )
        }

        Row(
            modifier = Modifier
                .padding(top = Dimens.spaceS)
                .padding(horizontal = Dimens.spaceM)
        ) {
            Text(
                "$songCount songs", style = MaterialTheme.typography.bodySmall,
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
                IconButton(onClick = onShufflePressed) {

                    Icon(Icons.Default.Shuffle, contentDescription = "shuffle")
                }

                Spacer(modifier = Modifier.width(Dimens.spaceM))
                IconButton(onClick = onMenuPressed) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "Menu")
                }
            }

            PlayPauseButton(
                onPressed = onPlayPressed,
                isPlaying = isPlaying,
                modifier = Modifier.padding(end = Dimens.spaceM)
            )
        }
    }
}

@DevicePreviews
@Composable
private fun SongCollectionHeaderNoArtworkPreview() {
    AppPreview {
        SongCollectionHeader(
            songCount = 42,
            isPlaying = false,
            onShufflePressed = {},
            onMenuPressed = {},
            onPlayPressed = {}
        )
    }
}

@DevicePreviews
@Composable
private fun SongCollectionHeaderWithArtworkPreview() {
    AppPreview {
        SongCollectionHeader(
            songCount = 12,
            isPlaying = true,
            onShufflePressed = {},
            onMenuPressed = {},
            onPlayPressed = {},
            title = "My Playlist",
            artwork = {
                CoverImage(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Dimens.Size.playlistCoverInset),
                    data = null
                )
            }
        )
    }
}
