package org.example.project.ui.component

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import org.example.project.core.model.Playlist
import org.example.project.core.model.PlaylistSong
import org.example.project.core.model.Song
import org.example.project.ui.theme.Dimens
import org.example.project.ui.theme.appColors

@Composable
fun PlaylistItem(
    modifier: Modifier = Modifier,
    playlist: Playlist,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Transparent)
            .clickable { onClick() }
            .alpha(1f)
            .padding(Dimens.spaceS),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Box(
            // 1. Center all children within the Box
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(Dimens.Size.coverThumb) // Match the size of the image
        ) {
            // 2. Draw the Image first (bottom layer)
            CoverImage(playlist.thumbnailUrl, modifier = modifier.fillMaxSize(), shape = RoundedCornerShape(Dimens.radiusS))

        }

        Spacer(Modifier.width(Dimens.spaceM))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = playlist.name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.SemiBold,
                color = appColors.textPrimary
            )
            Spacer(modifier = Modifier.height(Dimens.spaceXs))
            Text(
                text = "Playlist • ${playlist.songs.count()} songs",
                style = MaterialTheme.typography.bodySmall,
                color = appColors.textMuted
            )
        }
    }
}

@Preview
@Composable
private fun PlaylistItemPreview() {
    Surface {
        Column {
            PlaylistItem(
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
                ), onClick = {})
        }

    }
}
