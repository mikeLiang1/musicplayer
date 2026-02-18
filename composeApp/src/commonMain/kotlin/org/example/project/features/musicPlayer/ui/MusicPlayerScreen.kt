package org.example.project.features.musicPlayer.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
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
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.yield
import org.example.project.core.helper.formatTime
import org.example.project.core.model.Song
import org.schabi.newpipe.extractor.timeago.patterns.it

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
                    viewModel.onQueueClicked()
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
                onPlayPauseClick = viewModel::onPlayPauseClicked,
                onNextClick = viewModel::onNextClicked,
                onPreviousClick = viewModel::onPreviousClicked
            )

            Spacer(modifier = Modifier.height(16.dp))

            QueueSection(viewModel = viewModel)

//            LazyColumn {
//                itemsIndexed(
//                    items = state.upcomingQueue, key = { _, song -> song.url }
//                ) { index, song ->
//                    SongItem(song = song, isCurrentlyPlaying = index == 0) {
//                        viewModel.changePlayingToIndex(index)
//                    }
//                }
//            }
        }
    }
}

@Composable
fun PlayerControls(
    isPlaying: Boolean,
    onPlayPauseClick: () -> Unit,
    onNextClick: () -> Unit,
    onPreviousClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPreviousClick) {
            Icon(
                imageVector = Icons.Filled.SkipPrevious,
                contentDescription = "Previous"
            )
        }

        FilledIconButton(
            onClick = onPlayPauseClick,
            modifier = Modifier.size(64.dp)
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
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        AsyncImage(
            model = song.thumbnailUrl,
            contentDescription = null,
            modifier = Modifier.size(80.dp)
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

    val showHistory by remember {
        viewModel.uiState.map { it.showHistory }
    }.collectAsStateWithLifecycle(initialValue = false)

    val playerState by viewModel.playerState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    // 1. We manually track where the list should start visually.
    // Initialize it to the current index so it starts correctly.
    var visibleStartIndex by remember { mutableIntStateOf(playerState.currentIndex) }

    // 2. Derive the list based on our manual 'visibleStartIndex' (LAGGING STATE)
    // rather than the live 'playerState.currentIndex' (REAL STATE).
    val visibleSongs = remember(showHistory, visibleStartIndex) {
        if (showHistory) {
            playerState.queue
        } else {
            if (playerState.currentIndex < playerState.queue.size) {
                playerState.queue.subList(playerState.currentIndex, playerState.queue.size)
            } else {
                emptyList()
            }
        }
    }

    // When current index changes (i.e new song selected)
    LaunchedEffect(playerState.currentIndex) {
        if (showHistory) {
            // SHow history true means we have the whole list, so we can
            // directly scroll to the current index
            listState.animateScrollToItem(playerState.currentIndex)

            // updates starting position
            visibleStartIndex = playerState.currentIndex

            // updates visible song list
            viewModel.changeHistory(false)

            // History hidden
        } else {
            // CASE: We are moving to the NEXT song (Index 5 -> 6)
            if (playerState.currentIndex > visibleStartIndex) {
                // 1. The 'visibleSongs' list is currently still starting at 5 (Old Song).
                //    So Index 0 = Song 5, Index 1 = Song 6.

                // 2. Calculate where the new song is relative to our current cut list.
                val relativeIndex = playerState.currentIndex - visibleStartIndex

                // 3. Animate scroll to that item.
                //    User sees Song 5 scroll up and Song 6 arrive at the top.
                listState.animateScrollToItem(relativeIndex)

                // 4. NOW we update the list structure.
                //    We cut the list so it starts at 6.
                visibleStartIndex = playerState.currentIndex

            }
//            // CASE: We are moving to a PREVIOUS song (Index 6 -> 5)
            else if (playerState.currentIndex < visibleStartIndex) {
                // update the list first
                visibleStartIndex = playerState.currentIndex

                // Ensure the visibleList has enough time first
                delay(15)

                // 4. Now animate "up" to the new current song (Index 0).
                listState.animateScrollToItem(0)
            }
        }
    }

    Box(modifier = Modifier) {
        LazyColumn(state = listState) {
            itemsIndexed(
                items = visibleSongs,
                key = { _, song -> song.url }
            ) { index, song ->

                val absoluteIndex = if (showHistory) index else visibleStartIndex + index

                SongItem(
                    song = song,
                    isCurrentlyPlaying = absoluteIndex == playerState.currentIndex,
                ) {
                    viewModel.changePlayingToIndex(absoluteIndex)
                }
            }
        }


        // Simple chip - only shows when history is hidden and there are previous songs
        if (!showHistory && playerState.currentIndex > 0) {
            Surface(
                onClick = { viewModel.changeHistory(true) },
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp),
                tonalElevation = 4.dp,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(horizontal = 48.dp)
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
}
