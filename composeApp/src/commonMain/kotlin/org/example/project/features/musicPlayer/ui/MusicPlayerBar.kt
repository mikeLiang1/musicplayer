package org.example.project.features.musicPlayer.ui

import androidx.annotation.OptIn
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import kotlin.math.abs
import kotlin.math.roundToInt
import org.example.project.core.model.Song
import org.example.project.ui.component.CoverImage
import org.example.project.ui.theme.appColors

@OptIn(UnstableApi::class)
@Composable
fun MusicPlayerBar(viewModel: MusicPlayerViewModel, modifier: Modifier = Modifier) {
    val state by viewModel.playerState.collectAsStateWithLifecycle()
    val displayQueue by viewModel.displayQueue.collectAsStateWithLifecycle()

    val currentPosition by viewModel.currentPosition.collectAsStateWithLifecycle()

    val density = LocalDensity.current
    val swipeThresholdPx = with(density) { 56.dp.toPx() }
    val maxDragPx = with(density) { 120.dp.toPx() }
    // Live drag position — mutated synchronously in onDelta, never touched by a launched
    // coroutine, so there's no race with the release-time settle animation below
    var offsetPx by remember { mutableFloatStateOf(0f) }
    // Only used to drive the post-release settle/complete animation; reseeded from offsetPx
    // each time so it always continues smoothly from wherever the drag left off
    val settleAnimatable = remember { Animatable(0f) }
    val settleSpec = spring<Float>(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)
    val completeSpec = tween<Float>(durationMillis = 150)
    var songInfoWidthPx by remember { mutableFloatStateOf(0f) }

    displayQueue.current?.let { song ->
        val progress = (abs(offsetPx) / maxDragPx).coerceIn(0f, 1f)
        // -1 while swiping toward "next", 1 while swiping toward "previous", 0 at rest
        val direction = when {
            offsetPx < -0.5f -> -1
            offsetPx > 0.5f -> 1
            else -> 0
        }
        val previewSong = when (direction) {
            -1 -> displayQueue.manual.firstOrNull() ?: displayQueue.upcoming.firstOrNull()
            1 -> displayQueue.history.lastOrNull()
            else -> null
        }
        // Preview starts just past the song-info row's own edge (fully hidden) and slides in
        // to dock at 0 as drag progress reaches 1, so it visibly travels in from the side
        val previewOffsetPx = -direction * songInfoWidthPx * (1f - progress)

        Surface(
            color = appColors.backgroundElevated,
            modifier = modifier
                .height(65.dp)
                .clickable(indication = null, interactionSource = null) { viewModel.setFullScreen(true) }
                .draggable(
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState { delta ->
                        offsetPx = (offsetPx + delta).coerceIn(-maxDragPx, maxDragPx)
                    },
                    onDragStopped = {
                        val dragged = offsetPx
                        settleAnimatable.snapTo(dragged)
                        if (abs(dragged) > swipeThresholdPx) {
                            // Finish the slide in the direction it was already going
                            // (continues from the current position, no jump) so the
                            // preview is fully docked before we swap the underlying song
                            val target = if (dragged < 0) -maxDragPx else maxDragPx
                            settleAnimatable.animateTo(target, completeSpec) { offsetPx = value }
                            if (dragged < 0) viewModel.onNextClicked()
                            else viewModel.onPreviousClicked()
                            // Geometry already matches the new current song at offset 0,
                            // so resetting here doesn't produce any visible jump
                            offsetPx = 0f
                        } else {
                            settleAnimatable.animateTo(0f, settleSpec) { offsetPx = value }
                        }
                    }
                )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Song info with thumbnail — current song slides out while the
                    // adjacent song slides in from the opposite edge as you swipe
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .onSizeChanged { songInfoWidthPx = it.width.toFloat() }
                            .clipToBounds()
                    ) {
                        previewSong?.let { preview ->
                            SongInfoRow(
                                song = preview,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .offset { IntOffset(previewOffsetPx.roundToInt(), 0) }
                            )
                        }
                        SongInfoRow(
                            song = song,
                            modifier = Modifier
                                .fillMaxSize()
                                .offset { IntOffset(offsetPx.roundToInt(), 0) }
                                .alpha(1f - progress)
                        )
                    }

                    // Playback controls
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = viewModel::onPreviousClicked
                        ) {
                            Icon(
                                imageVector = Icons.Filled.SkipPrevious,
                                contentDescription = "Previous",
                                tint = appColors.iconPrimary
                            )
                        }
                        // Play/Pause button
                        IconButton(
                            onClick = viewModel::onPlayPauseClicked
                        ) {
                            Icon(
                                imageVector =
                                    if (state.isBuffering) Icons.Default.Refresh else {
                                        if (state.isPlaying) {
                                            Icons.Filled.Pause
                                        } else {
                                            Icons.Filled.PlayArrow
                                        }
                                    },
                                contentDescription = if (state.isPlaying) "Pause" else "Play",
                                tint = appColors.iconPrimary
                            )
                        }

                        // Next button
                        IconButton(
                            onClick = viewModel::onNextClicked
                        ) {
                            Icon(
                                imageVector = Icons.Filled.SkipNext,
                                contentDescription = "Next",
                                tint = appColors.iconPrimary
                            )
                        }
                    }
                }
                LinearProgressIndicator(
                    progress = {
                        if (song.duration > 0) currentPosition.toFloat() / song.duration.toFloat()
                        else 0f
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter),
                    color = appColors.accentDark,
                    trackColor = appColors.backgroundSurface,
                )
            }
        }
    }
}

@Composable
private fun SongInfoRow(song: Song, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CoverImage(song.thumbnailUrl, size = 48.dp, shape = RoundedCornerShape(4.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = appColors.textPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = song.artist,
                style = MaterialTheme.typography.bodySmall,
                color = appColors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
