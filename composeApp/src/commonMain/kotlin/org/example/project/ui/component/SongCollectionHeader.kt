package org.example.project.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
    onMenuPressed: (() -> Unit)?,
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
                fontWeight = FontWeight.SemiBold,
                color = appColors.textPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(
                    horizontal = Dimens.spaceM,
                    vertical = Dimens.spaceS
                )
            )
        }

        // Text sits on the same 12dp gutter as the SongItem rows below.
        Text(
            text = if (songCount == 1) "1 song" else "$songCount songs",
            style = MaterialTheme.typography.bodySmall,
            color = appColors.textMuted,
            modifier = Modifier.padding(horizontal = Dimens.spaceM)
        )
        // TODO: Total duration ?

        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = Dimens.spaceS)
                // Right gutter only: on the left, IconButton's own 12dp inset already lands
                // the glyph on the same gutter as the text above and the rows below.
                .padding(end = Dimens.spaceM)
        ) {
            // No spacer between the two — adjacent 48dp touch targets are already spaced,
            // and an extra gap makes the pair read as two unrelated controls.
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onShufflePressed) {
                    Icon(
                        Icons.Default.Shuffle,
                        contentDescription = "shuffle",
                        tint = appColors.iconSecondary
                    )
                }
                // Null when the collection has no menu-worthy actions (an empty Liked Songs) —
                // a ⋮ that opens an empty sheet is worse than no ⋮ at all.
                if (onMenuPressed != null) {
                    IconButton(onClick = onMenuPressed) {
                        Icon(
                            Icons.Filled.MoreVert,
                            contentDescription = "Menu",
                            tint = appColors.iconSecondary
                        )
                    }
                }
            }

            PlayPauseButton(
                onPressed = onPlayPressed,
                isPlaying = isPlaying
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
