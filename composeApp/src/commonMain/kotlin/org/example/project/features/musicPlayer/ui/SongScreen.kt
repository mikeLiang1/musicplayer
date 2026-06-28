package org.example.project.features.musicPlayer.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.example.project.core.helper.formatTime
import org.example.project.core.model.Song
import org.example.project.ui.component.CoverImage
import org.example.project.ui.theme.appColors
import kotlin.math.roundToInt

@Composable
fun SongScreen(song: Song?, viewModel: MusicPlayerViewModel) {
    val displayQueue by viewModel.displayQueue.collectAsStateWithLifecycle()

    song?.let { current ->
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            SongPager(
                queue = displayQueue,
                onSongSelected = viewModel::changePlayingToSong,
                modifier = Modifier
                    .padding(vertical = 8.dp)
                    .size(320.dp)
            ) { song ->
                CoverImage(
                    data = song.thumbnailUrl,
                    size = 320.dp,
                    shape = RoundedCornerShape(32.dp)
                )
            }

            SongDetails(
                song = current,
                viewModel = viewModel,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp)
            )
        }
    }
}


@Composable
private fun SongDetails(song: Song, viewModel: MusicPlayerViewModel, modifier: Modifier = Modifier) {
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
    viewModel: MusicPlayerViewModel,
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

    val thumbSize by animateDpAsState(
        targetValue = if (isSliding) 20.dp else 14.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "thumbSize"
    )

    LaunchedEffect(currentPosition) {
        if (!isSliding && duration > 0) {
            sliderPosition = currentPosition.toFloat() / duration.toFloat()
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(28.dp)
        ) {
            val trackWidth = constraints.maxWidth.toFloat()
            val thumbSizePx = with(LocalDensity.current) { thumbSize.toPx() }
            val thumbOffset = (sliderPosition * trackWidth - thumbSizePx / 2).coerceAtLeast(0f)
            val fillWidth = (sliderPosition * trackWidth).coerceIn(0f, trackWidth)

            // Track background with tap to seek
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(20.dp)
                    .align(Alignment.Center)
                    .pointerInput(duration) {
                        detectTapGestures { offset ->
                            sliderPosition = (offset.x / trackWidth).coerceIn(0f, 1f)
                            onSeek((sliderPosition * duration).toLong())
                        }
                    }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(appColors.backgroundSurface)
                        .align(Alignment.Center)
                )
            }

            // Filled portion
            Box(
                modifier = Modifier
                    .width(with(LocalDensity.current) { fillWidth.toDp() })
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(appColors.accentDark, appColors.accentPrimary)
                        )
                    )
                    .align(Alignment.CenterStart)
            )

            // Thumb
            Box(
                modifier = Modifier
                    .offset { IntOffset(thumbOffset.roundToInt(), 0) }
                    .size(thumbSize)
                    .shadow(4.dp, CircleShape)
                    .background(appColors.iconPrimary, CircleShape)
                    .align(Alignment.CenterStart)
                    .pointerInput(duration) {
                        detectHorizontalDragGestures(
                            onDragStart = { isSliding = true },
                            onDragEnd = {
                                isSliding = false
                                onSeek((sliderPosition * duration).toLong())
                            },
                            onHorizontalDrag = { _, dragAmount ->
                                val delta = dragAmount / trackWidth
                                sliderPosition = (sliderPosition + delta).coerceIn(0f, 1f)
                            }
                        )
                    }
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatTime(if (isSliding) (sliderPosition * duration).toLong() else currentPosition),
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
