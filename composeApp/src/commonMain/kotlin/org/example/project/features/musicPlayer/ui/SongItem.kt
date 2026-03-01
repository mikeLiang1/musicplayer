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
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MoreVert
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
    song: Song,
    isCurrentlyPlaying: Boolean = false,
    alpha: Float = 1f,
    onMenuClicked: () -> Unit,
    onClick: () -> Unit
) {
    val backgroundColor = when {
        isCurrentlyPlaying -> appColors.accentContainer.copy(alpha = 0.3f)
        song.isManual -> appColors.rose.copy(alpha = 0.1f)
        else -> Color.Transparent
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .clickable { onClick() }
            .alpha(alpha)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
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

        IconButton(
            onClick = { onMenuClicked() }
        ) {
            Icon(
                imageVector = Icons.Filled.MoreVert,
                contentDescription = "SongMenu",
                tint = appColors.iconSecondary
            )
        }
    }
}

@Preview
@Composable
fun SongItemPreview() {
    Surface {
        SongItem(
            song = Song(
                url = "item.url",
                title = "title",
                artist = "Unknown",
                thumbnailUrl = "item.thumbnails.firstOrNull()?.url",
                duration = 3000L
            ), isCurrentlyPlaying = true, onMenuClicked = {}, onClick = {}
        )

        Spacer(Modifier.height(12.dp))

        SongItem(
            song = Song(
                url = "item.url",
                title = "title",
                artist = "Unknown",
                thumbnailUrl = "item.thumbnails.firstOrNull()?.url",
                duration = 3000L
            ),
            isCurrentlyPlaying = false, onMenuClicked = {},
            onClick = {})

        SongItem(
            song = Song(
                url = "item.url",
                title = "title",
                artist = "Unknown",
                thumbnailUrl = "item.thumbnails.firstOrNull()?.url",
                duration = 3000L,
                isManual = true
            ),
            isCurrentlyPlaying = false, onMenuClicked = {},
            onClick = {})
    }
}

