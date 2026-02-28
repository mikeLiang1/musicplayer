package org.example.project.features.musicPlayer.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.google.common.math.LinearTransformation.horizontal
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import org.example.project.core.helper.formatTime
import org.example.project.core.model.FlowMode
import org.example.project.core.model.Song

@Composable
fun MusicPlayerScreen(
    viewModel: MusicPlayerViewModel,
    navigateBack: () -> Unit
) {
    BackHandler { navigateBack() }

    val state by viewModel.playerState.collectAsStateWithLifecycle()

    state.currentSong?.let { song ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
                .padding(horizontal = 16.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth()) {
                IconButton(
                    onClick = { navigateBack() }) {
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowDown,
                        contentDescription = "Close"
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = {

                }
                ) {
                    Icon(
                        imageVector = Icons.Filled.Menu,
                        contentDescription = "Menu"
                    )
                }
            }
            // Current song info
            SongInfo(song = song)

            Spacer(modifier = Modifier.height(16.dp))

            // Progress slider
            MusicPlayerProgressSlider(
                viewModel,
                duration = song.duration
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Player controls
            PlayerControls(
                isPlaying = state.isPlaying,
                isShuffled = state.isShuffled,
                flowMode = state.flowMode,
                onPlayPauseClick = viewModel::onPlayPauseClicked,
                onNextClick = viewModel::onNextClicked,
                onPreviousClick = viewModel::onPreviousClicked,
                onShuffleClicked = viewModel::changeShuffleOption,
                onFlowClicked = viewModel::cycleFlowMode
            )

            Spacer(modifier = Modifier.height(16.dp))

            QueueSection(viewModel = viewModel)

        }
    }
}

@Composable
fun PlayerControls(
    modifier: Modifier = Modifier,
    isPlaying: Boolean,
    isShuffled: Boolean,
    flowMode: FlowMode,
    onPlayPauseClick: () -> Unit,
    onNextClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onShuffleClicked: () -> Unit,
    onFlowClicked: () -> Unit
) {
    var showFlowChip by remember { mutableStateOf(false) }
    var isFirstLaunch by remember { mutableStateOf(true) }

    LaunchedEffect(flowMode) {
        if (isFirstLaunch) {
            isFirstLaunch = false
            return@LaunchedEffect
        }
        showFlowChip = true
        delay(2000)
        showFlowChip = false
    }
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = modifier
        ) {
            IconButton(onClick = onShuffleClicked) {
                Icon(
                    imageVector = Icons.Default.Shuffle,
                    contentDescription = "Shuffle",
                    tint = if (isShuffled) MaterialTheme.colorScheme.primary else LocalContentColor.current
                )
                if (isShuffled) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .offset(y = (-4).dp)
                            .size(4.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                    )
                }
            }
        }

        IconButton(onClick = onPreviousClick) {
            Icon(
                imageVector = Icons.Filled.SkipPrevious,
                contentDescription = "Previous"
            )
        }

        FilledIconButton(
            onClick = onPlayPauseClick,
            modifier = Modifier.size(64.dp),
            colors = IconButtonDefaults.filledIconButtonColors(
                contentColor = MaterialTheme.colorScheme.onPrimary
            )

        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Play",
                modifier = Modifier.size(32.dp)
            )
        }

        IconButton(onClick = onNextClick) {
            Icon(
                imageVector = Icons.Filled.SkipNext,
                contentDescription = "Next"
            )
        }
        Box(contentAlignment = Alignment.Center) {
            if (showFlowChip) {
                Popup(
                    alignment = Alignment.BottomCenter,
                    offset = IntOffset(0, -120)
                ) {
                    SuggestionChip(
                        onClick = {},
                        label = { Text(flowMode.label) },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = MaterialTheme.colorScheme.surfaceBright,
                        ),
                        border = null
                    )
                }
            }
            IconButton(onClick = onFlowClicked) {
                Icon(
                    imageVector = flowMode.icon,
                    contentDescription = "Flow",
                    tint = if (flowMode != FlowMode.STOP_AT_END) MaterialTheme.colorScheme.primary else LocalContentColor.current
                )
            }
        }

    }

}

@Composable
fun MusicPlayerProgressSlider(
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
fun ProgressSlider(
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
        Slider(
            value = sliderPosition,
            onValueChange = {
                isSliding = true
                sliderPosition = it
            },
            onValueChangeFinished = {
                isSliding = false
                onSeek((sliderPosition * duration).toLong())
            }
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatTime(currentPosition),
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = formatTime(duration),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
fun SongInfo(
    song: Song,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically

    ) {
        AsyncImage(
            model = song.thumbnailUrl,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            contentScale = ContentScale.Crop
        )

        Column {
            Text(
                text = song.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = song.artist,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// Composable - much simpler now
@Composable
private fun QueueSection(viewModel: MusicPlayerViewModel) {

    val playerState by viewModel.playerState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // 1. We manually track where the list should start visually.
    // Initialize it to the current index so it starts correctly.
    var visibleStartIndex by remember { mutableIntStateOf(playerState.currentIndex + 1) }


    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is MusicPlayerEffect.ScrollUp -> {
                    // Force the next composition to stay (since when chip removed, first song will be at index 0)
                    listState.requestScrollToItem(playerState.currentIndex - 1)
                    // Make full list available
                    viewModel.changeHistory(true)
                    visibleStartIndex = 0

                    // scroll to 2.5 items
                    val targetIndex = (playerState.currentIndex - 2).coerceAtLeast(0)
                    val itemHeight = listState.layoutInfo.visibleItemsInfo.firstOrNull()?.size ?: 0
                    try {
                        listState.animateScrollToItem(targetIndex, -itemHeight / 2)
                    } catch (e: CancellationException) {
                        // Swallow to avoid bugs
                    }
                }
            }
        }
    }


    // 2. Derive the list based on our manual 'visibleStartIndex' (LAGGING STATE)
    // rather than the live 'playerState.currentIndex' (REAL STATE).
    // TODO: maybe not use queue directly ?
    val visibleSongs =
        remember(uiState.showHistory, visibleStartIndex, playerState.manualItemCount, playerState.isShuffled) {
            if (uiState.showHistory) {
                playerState.queue
            } else {
                playerState.queue.drop(playerState.currentIndex.coerceIn(0, playerState.queue.size))
            }
        }

    // When current index changes (i.e new song selected)
    LaunchedEffect(playerState.currentIndex) {
        val relativeIndex =
            if (playerState.manualItemCount > 0) 0 else playerState.currentIndex - visibleStartIndex
        if (uiState.showHistory) {
            try {
                // SHow history true means we have the whole list, so we can
                // directly scroll to the current index
                listState.animateScrollToItem(relativeIndex)
            } finally {
                withFrameNanos { }
                // updates starting position
                visibleStartIndex = playerState.currentIndex

                // updates visible song list
                viewModel.changeHistory(false)
            }
            // TODO: We can remove animate scroll to item and let songItem.animateItem choose the animation potenietally
        } else {
            // CASE: We are moving to the NEXT song (Index 5 -> 6)
            if (playerState.currentIndex > visibleStartIndex) {

                try {
                    // Animate while list still has old content
                    listState.animateScrollToItem(relativeIndex + 1)
                } finally {
                    // Then update list structure
                    withFrameNanos { }
                    visibleStartIndex = playerState.currentIndex
                }

            }
            // CASE: We are moving to a PREVIOUS song (Index 6 -> 5)
            else if (playerState.currentIndex < visibleStartIndex) {
                val index = if (playerState.currentIndex == 0) 0 else 1
                // Expand list to include previous songs
                visibleStartIndex = playerState.currentIndex

                withFrameNanos { }

                try {
                    // Now animate up to the new current song
                    listState.animateScrollToItem(index)
                } catch (e: CancellationException) {

                }
            }
        }
    }

    LazyColumn(state = listState, horizontalAlignment = Alignment.CenterHorizontally) {
        if (!uiState.showHistory && playerState.currentIndex > 0) {
            item {
                Surface(
                    onClick = {
                        viewModel.scrollWhenHistoryOpened()
                    },
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp),
                    tonalElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowUp,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "${playerState.currentIndex} previous songs",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }

        itemsIndexed(
            items = visibleSongs,
            key = { _, song -> song.uniqueId }
        ) { index, song ->

            val absoluteIndex = if (uiState.showHistory) index else visibleStartIndex + index
            val isPreviousSong = absoluteIndex < playerState.currentIndex

            SongItem(
                modifier = Modifier.animateItem(),
                song = song,
                isCurrentlyPlaying = absoluteIndex == playerState.currentIndex,
                onMenuClicked = { viewModel.onMenuClicked(song) },
                alpha = if (isPreviousSong) 0.6f else 1f
            ) {
                viewModel.changePlayingToIndex(absoluteIndex)
            }
        }

    }
}
