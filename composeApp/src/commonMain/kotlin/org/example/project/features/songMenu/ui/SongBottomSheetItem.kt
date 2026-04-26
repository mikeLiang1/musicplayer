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
import androidx.compose.material.icons.outlined.PlaylistRemove
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Queue
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.example.project.ui.theme.appColors

@Composable
fun BottomSheetItem(
    modifier: Modifier = Modifier,
    songMenuAction: SongMenuAction,
    onClick: () -> Unit
) {
    val isManual = songMenuAction == SongMenuAction.RemoveFromQueue
    Row(
        modifier = modifier
            .background(appColors.backgroundElevated)
            .clickable(onClick = { onClick() })
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(appColors.accentPrimary.copy(alpha = 0.15f))
                .padding(8.dp)
        ) {
            Icon(
                songMenuAction.icon,
                contentDescription = songMenuAction.label,
                tint = if (isManual) appColors.error else appColors.iconSecondary
            )
        }
        Text(
            text = songMenuAction.label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = if (isManual) appColors.error else appColors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Preview
@Composable
private fun BottomSheetItemPreview() {
    Surface {
        Column {
            BottomSheetItem(
                songMenuAction = SongMenuAction.AddToPlaylist, onClick = {}
            )
            BottomSheetItem(
                songMenuAction = SongMenuAction.AddToPlaylist, onClick = {}
            )
        }
    }
}

sealed class SongMenuAction {
    abstract val label: String
    abstract val icon: ImageVector

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
    }

    companion object {
        val all = listOf(
            AddToQueue,
            AddToPlaylist,
            GoToArtist,
            GoToAlbum,
            RemoveFromQueue
        )
    }
}
