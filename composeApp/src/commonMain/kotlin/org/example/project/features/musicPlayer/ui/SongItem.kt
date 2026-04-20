package org.example.project.features.musicPlayer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Reorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.example.project.core.helper.formatTime
import org.example.project.core.model.Song
import org.example.project.ui.component.CoverImage
import org.example.project.ui.theme.appColors

@Composable
fun SongItem(
    modifier: Modifier = Modifier,
    dragHandleModifier: Modifier = Modifier,
    song: Song,
    isCurrentlyPlaying: Boolean = false,
    isPreviousSong: Boolean = false,
    isEditable: Boolean = false,
    showRemoveButton: Boolean = false,
    isManual: Boolean = false,
    onMenuClicked: () -> Unit = {},
    onRemoveClicked: () -> Unit = {},
    onClick: () -> Unit
) {
    val backgroundColor = when {
        isCurrentlyPlaying -> appColors.accentContainer.copy(alpha = 0.3f)
        isManual -> appColors.rose.copy(alpha = 0.1f)
        else -> Color.Transparent
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .clickable { onClick() }
            .alpha(if (isPreviousSong) 0.45f else 1f)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        if (isEditable) {
            Icon(
                imageVector = Icons.Default.Reorder, // The "hamburger" or "drag" handle
                contentDescription = "Drag to reorder",
                tint = appColors.iconSecondary,
                modifier = Modifier
                    .padding(end = 16.dp)
                    .then(dragHandleModifier)
            )
        }

        Box(
            // 1. Center all children within the Box
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(50.dp) // Match the size of the image
        ) {
            // 2. Draw the Image first (bottom layer)
            CoverImage(song.thumbnailUrl, modifier = modifier.fillMaxSize(), shape = RoundedCornerShape(4.dp))

            // 3. Draw the Icon second (top layer)
            if (isCurrentlyPlaying) {
                Icon(
                    imageVector = Icons.Filled.GraphicEq,
                    contentDescription = "Currently playing",
                    tint = appColors.iconActive,
                    modifier = Modifier.size(24.dp) // Adjust icon size as needed
                )
            }
        }

        Spacer(Modifier.width(12.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = song.title,
                maxLines = 1,
                fontWeight = FontWeight.SemiBold,
                color = when {
                    isCurrentlyPlaying -> appColors.accentPrimary
                    else -> appColors.textPrimary
                }
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${song.artist} • ${formatTime(song.duration)}",
                style = MaterialTheme.typography.bodySmall,
                color = appColors.textMuted
            )
        }

        if (showRemoveButton) {
            IconButton(
                onClick = { onRemoveClicked() }
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Remove song",
                    tint = appColors.iconSecondary
                )
            }
        } else {
            IconButton(
                onClick = { onMenuClicked() }
            ) {
                Icon(
                    imageVector = Icons.Filled.MoreVert,
                    contentDescription = "Song menu",
                    tint = appColors.iconSecondary
                )
            }
        }
    }
}

@Preview
@Composable
private fun SongItemRefactoredPreview() {
    Surface {
        Column {
            SongItem(
                song = Song(
                    url = "item.url",
                    title = "Currently Playing Song",
                    artist = "Artist",
                    thumbnailUrl = "item.thumbnails.firstOrNull()?.url",
                    duration = 3000L
                ),
                isCurrentlyPlaying = true,
                onMenuClicked = {},
                onClick = {}
            )

            Spacer(Modifier.height(12.dp))

            SongItem(
                song = Song(
                    url = "item.url",
                    title = "Manual Queue Song",
                    artist = "Artist",
                    thumbnailUrl = "item.thumbnails.firstOrNull()?.url",
                    duration = 3000L,
                ),
                showRemoveButton = true,
                onMenuClicked = {},
                onRemoveClicked = {},
                onClick = {}
            )

            Spacer(Modifier.height(12.dp))

            SongItem(
                song = Song(
                    url = "item.url",
                    title = "Normal Queue Song",
                    artist = "Artist",
                    thumbnailUrl = "item.thumbnails.firstOrNull()?.url",
                    duration = 3000L
                ),
                isEditable = true,
                showRemoveButton = true,
                onMenuClicked = {},
                onRemoveClicked = {},
                onClick = {}
            )

            Spacer(Modifier.height(12.dp))

            SongItem(
                song = Song(
                    url = "item.url",
                    title = "History Song",
                    artist = "Artist",
                    thumbnailUrl = "item.thumbnails.firstOrNull()?.url",
                    duration = 3000L
                ),
                isPreviousSong = true,
                onMenuClicked = {},
                onClick = {}
            )
        }

    }
}
