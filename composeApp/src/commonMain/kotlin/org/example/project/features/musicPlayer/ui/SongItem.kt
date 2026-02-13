package org.example.project.features.musicPlayer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import org.example.project.core.helper.formatTime
import org.example.project.core.model.Song

@Composable
fun SongItem(
    song: Song,
    isCurrentlyPlaying: Boolean = false,
    onClick: () -> Unit
) {
    val backgroundColor =
        if (isCurrentlyPlaying)
            MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
        else
            Color.Transparent

    val titleColor =
        if (isCurrentlyPlaying)
            MaterialTheme.colorScheme.primary
        else
            MaterialTheme.colorScheme.onSurface

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor, RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = song.thumbnailUrl,
            contentDescription = null,
            modifier = Modifier
                .size(50.dp)
                .clip(RoundedCornerShape(4.dp))
        )

        Spacer(Modifier.width(12.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = song.title,
                maxLines = 1,
                fontWeight = FontWeight.Bold,
                color = titleColor
            )

            Text(
                text = "${song.artist} • ${formatTime(song.duration)}",
                style = MaterialTheme.typography.bodySmall
            )
        }

        if (isCurrentlyPlaying) {
            Icon(
                imageVector = Icons.Filled.GraphicEq,
                contentDescription = "Currently playing",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

