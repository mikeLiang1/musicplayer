package org.example.project.features.musicPlayer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.example.project.core.helper.formatTime
import org.example.project.core.model.Song
import org.example.project.ui.component.CoverImage
import org.example.project.ui.theme.appColors

@Composable
fun SongScreen(song: Song, viewModel: MusicPlayerViewModelRefactored) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {

        CoverImage(
            data = song.thumbnailUrl,
            size = 320.dp,
            shape = RoundedCornerShape(32.dp),
            modifier = Modifier.padding(vertical = 8.dp)
        )

        SongDetails(
            song = song,
            viewModel = viewModel,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp)
        )
    }
}


@Composable
private fun SongDetails(song: Song, viewModel: MusicPlayerViewModelRefactored, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        SongInfoRow(song.title, song.artist)

        Spacer(modifier = Modifier.height(24.dp))

        MusicPlayerProgressSlider(
            viewModel,
            duration = song.duration
        )

    }
}

@Composable
private fun SongInfoRow(title: String, artist: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = appColors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = artist,
                style = MaterialTheme.typography.bodyMedium,
                color = appColors.textSecondary
            )
        }
        IconButton(onClick = { }) {
            Icon(
                imageVector = Icons.Filled.Favorite,
                contentDescription = "Favorite",
                tint = appColors.rose,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}


@Composable
private fun MusicPlayerProgressSlider(
    viewModel: MusicPlayerViewModelRefactored,
    duration: Long,
    modifier: Modifier = Modifier
) {
    val currentPosition by viewModel.currentPosition.collectAsStateWithLifecycle()

    ProgressSlider(
        currentPosition = currentPosition,
        duration = duration,
        onSeek = viewModel::onSeekTo,
        modifier = modifier
    )
}

@Composable
private fun ProgressSlider(
    currentPosition: Long,
    duration: Long,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var sliderPosition by remember { mutableFloatStateOf(0f) }
    var isSliding by remember { mutableStateOf(false) }

    LaunchedEffect(currentPosition) {
        if (!isSliding && duration > 0) {
            sliderPosition = currentPosition.toFloat() / duration.toFloat()
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp), // tall touch target
            contentAlignment = Alignment.Center
        ) {
            // Track
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(appColors.backgroundSurface)
            )

            // Fill + thumb
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.CenterStart
            ) {
                // Filled portion
                Box(
                    modifier = Modifier
                        .fillMaxWidth(sliderPosition)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(appColors.accentDark, appColors.accentPrimary)
                            )
                        )
                )

                // Thumb dot
                Box(
                    modifier = Modifier
                        .fillMaxWidth(sliderPosition)
                        .wrapContentWidth(Alignment.End)
                ) {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .shadow(4.dp, CircleShape)
                            .background(appColors.iconPrimary, CircleShape)
                    )
                }
            }

            // Invisible drag surface
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(duration) {
                        detectHorizontalDragGestures(
                            onDragStart = { isSliding = true },
                            onDragEnd = {
                                isSliding = false
                                onSeek((sliderPosition * duration).toLong())
                            },
                            onHorizontalDrag = { _, dragAmount ->
                                val delta = dragAmount / size.width
                                sliderPosition = (sliderPosition + delta).coerceIn(0f, 1f)
                            }
                        )
                    }
                    .pointerInput(duration) {
                        detectTapGestures { offset ->
                            sliderPosition = (offset.x / size.width).coerceIn(0f, 1f)
                            onSeek((sliderPosition * duration).toLong())
                        }
                    }
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatTime(currentPosition),
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                color = appColors.textMuted
            )
            Text(
                text = formatTime(duration),
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                color = appColors.textMuted
            )
        }
    }
}
