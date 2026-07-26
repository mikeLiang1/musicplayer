package org.example.project.features.songMenu.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.PlaylistRemove
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Queue
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import org.example.project.ui.theme.AppPreview
import org.example.project.ui.theme.DevicePreviews
import org.example.project.ui.theme.Dimens
import org.example.project.ui.theme.appColors

@Composable
fun BottomSheetItem(
    modifier: Modifier = Modifier,
    songMenuAction: SongMenuAction,
    onClick: () -> Unit
) {
    val contentColor = when (songMenuAction.accent) {
        MenuAccent.Neutral -> appColors.iconSecondary
        MenuAccent.Like -> appColors.rose
        MenuAccent.Destructive -> appColors.error
    }
    val chipColor = when (songMenuAction.accent) {
        MenuAccent.Neutral -> appColors.accentPrimary.copy(alpha = 0.15f)
        MenuAccent.Like -> appColors.rose.copy(alpha = 0.15f)
        MenuAccent.Destructive -> appColors.error.copy(alpha = 0.15f)
    }
    Row(
        modifier = modifier
            .background(appColors.backgroundElevated)
            .clickable(onClick = { onClick() })
            .padding(horizontal = Dimens.spaceL, vertical = Dimens.spaceM)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.spaceM)
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(Dimens.radiusM))
                .background(chipColor)
                .padding(Dimens.spaceS)
        ) {
            Icon(
                songMenuAction.icon,
                contentDescription = songMenuAction.label,
                tint = contentColor
            )
        }
        Text(
            text = songMenuAction.label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = if (songMenuAction.accent == MenuAccent.Neutral) {
                appColors.textPrimary
            } else {
                contentColor
            },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@DevicePreviews
@Composable
private fun BottomSheetItemPreview() {
    AppPreview {
        Column {
            BottomSheetItem(songMenuAction = SongMenuAction.Like, onClick = {})
            BottomSheetItem(songMenuAction = SongMenuAction.Unlike, onClick = {})
            BottomSheetItem(songMenuAction = SongMenuAction.AddToPlaylist, onClick = {})
            BottomSheetItem(songMenuAction = SongMenuAction.RemoveFromQueue, onClick = {})
        }
    }
}

/** Colour role for a menu row — see [BottomSheetItem] for how each maps to icon/chip/label. */
enum class MenuAccent { Neutral, Like, Destructive }

sealed class SongMenuAction {
    abstract val label: String
    abstract val icon: ImageVector
    open val accent: MenuAccent = MenuAccent.Neutral

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
