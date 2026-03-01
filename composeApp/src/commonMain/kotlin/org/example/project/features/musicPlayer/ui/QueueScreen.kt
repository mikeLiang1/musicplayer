package org.example.project.features.musicPlayer.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.CancellationException
import org.example.project.core.helper.formatTime
import org.example.project.core.model.FlowMode
import org.example.project.core.model.Song
import org.example.project.ui.component.CoverImage
import org.example.project.ui.theme.appColors

@Composable
fun QueueSection(viewModel: MusicPlayerViewModel) {

    val playerState by viewModel.playerState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var queueUpdateKey by remember { mutableIntStateOf(0) }


    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is MusicPlayerEffect.ScrollUp -> {
                    listState.requestScrollToItem(playerState.currentIndex)
                    viewModel.changeHistory(true)
                    val targetIndex = (playerState.currentIndex - 2).coerceAtLeast(0)
                    val itemHeight = listState.layoutInfo.visibleItemsInfo.firstOrNull()?.size ?: 0
                    try {
                        listState.animateScrollToItem(targetIndex, -itemHeight / 2)
                    } catch (e: CancellationException) {
                    }
                }
            }
        }
    }

    val visibleSongs =
        remember(
            uiState.showHistory,
            playerState.isShuffled,
            queueUpdateKey
        ) {
            if (uiState.showHistory) {
                playerState.queue
            } else {
                playerState.queue.drop((playerState.currentIndex + 1).coerceIn(0, playerState.queue.size))
            }
        }

    LaunchedEffect(playerState.currentIndex, playerState.manualItemCount) {
        if (!uiState.showHistory) {
            try {
                withFrameNanos { }
                withFrameNanos { }
                queueUpdateKey++ // recompute list with new data
                withFrameNanos { } // wait for recomposition with new list
                listState.animateScrollToItem(0)
            } catch (e: CancellationException) {
            }
            // This way didnt work for previous song, in that case needed to call queue update key first
//            try {
//                listState.animateScrollToItem(0)
//
//            } finally {
//                queueUpdateKey++
//            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {

        // ── Pinned current song ──────────────────────────
        playerState.currentSong?.let { currentSong ->
            CurrentSongRow(song = currentSong)

            // Next up divider
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    color = appColors.divider
                )
                Text(
                    text = "Next up",
                    style = MaterialTheme.typography.labelSmall,
                    color = appColors.textDim,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 0.14.sp
                )
                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    color = appColors.divider
                )
            }
        }

        // ── Scrollable queue ─────────────────────────────
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f)
        ) {
            itemsIndexed(
                items = visibleSongs,
                key = { _, song -> song.uniqueId }
            ) { index, song ->
                val absoluteIndex = if (uiState.showHistory) index else playerState.currentIndex + 1 + index
                val isPreviousSong = absoluteIndex < playerState.currentIndex

                SongItem(
                    modifier = Modifier.animateItem(),
                    song = song,
                    isCurrentlyPlaying = absoluteIndex == playerState.currentIndex, // current is pinned above, never in list
                    onMenuClicked = { viewModel.onMenuClicked(song) },
                    alpha = if (isPreviousSong) 0.45f else 1f,
//                    showQueuedBadge = song.isManual
                ) {
                    viewModel.changeHistory(false)
                    viewModel.changePlayingToIndex(absoluteIndex)
                }
            }

            // Flow mode footer as last list item
            item {
                FlowModeFooter(flowMode = playerState.flowMode)
            }
        }
    }
}

@Composable
private fun CurrentSongRow(song: Song) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(appColors.accentContainer.copy(alpha = 0.12f))
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // artwork with eq overlay
        Box(modifier = Modifier.size(48.dp)) {
            CoverImage(
                data = song.thumbnailUrl,
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(8.dp)
            )
            // eq bars overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.45f)),
                contentAlignment = Alignment.Center
            ) {
                EqualizerBars()
            }
        }

        // Left accent bar
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(48.dp)
                .clip(RoundedCornerShape(99.dp))
                .background(appColors.accentPrimary)
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = appColors.accentPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${song.artist} • ${formatTime(song.duration)}",
                style = MaterialTheme.typography.bodySmall,
                color = appColors.textMuted
            )
        }

        Icon(
            imageVector = Icons.Rounded.MoreVert,
            contentDescription = null,
            tint = appColors.iconMuted,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun EqualizerBars() {
    val infiniteTransition = rememberInfiniteTransition(label = "eq")
    val bars = listOf(0.3f, 0.7f, 1f, 0.5f)

    Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom,
        modifier = Modifier.height(20.dp)
    ) {
        bars.forEachIndexed { i, base ->
            val scale by infiniteTransition.animateFloat(
                initialValue = base * 0.3f,
                targetValue = base,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = 500 + i * 80,
                        easing = FastOutSlowInEasing
                    ),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "bar$i"
            )
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .fillMaxHeight(scale)
                    .clip(RoundedCornerShape(2.dp))
                    .background(appColors.accentPrimary)
            )
        }
    }
}

@Composable
private fun FlowModeFooter(flowMode: FlowMode) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(appColors.accentContainer.copy(alpha = 0.3f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = flowMode.icon,
                contentDescription = null,
                tint = appColors.accentPrimary,
                modifier = Modifier.size(14.dp)
            )
        }
        Column {
            Text(
                text = flowMode.label,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = appColors.textMuted
            )
            Text(
                text = when (flowMode) {
                    FlowMode.STOP_AT_END -> "Playback stops after last song"
                    FlowMode.REPEAT_ALL -> "Will loop back to start"
                    FlowMode.INFINITE -> "Finding similar songs…"
                },
                style = MaterialTheme.typography.labelSmall,
                color = appColors.textDim
            )
        }
    }
}
