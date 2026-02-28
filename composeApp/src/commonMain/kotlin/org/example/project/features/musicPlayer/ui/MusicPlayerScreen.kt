package org.example.project.features.musicPlayer.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.example.project.core.helper.formatTime
import org.example.project.core.model.FlowMode
import org.example.project.core.model.PlayerState
import org.example.project.core.model.Song
import org.example.project.ui.component.CoverImage
import org.example.project.ui.theme.appColors

@Composable
fun MusicPlayerScreen(
    viewModel: MusicPlayerViewModel,
    navigateBack: () -> Unit
) {
    BackHandler { navigateBack() }

    val state by viewModel.playerState.collectAsStateWithLifecycle()
    val pagerState = rememberPagerState(pageCount = { 2 })

    state.currentSong?.let { song ->
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()

        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f),
                userScrollEnabled = false
            ) { page ->
                when (page) {
                    0 -> SongScreen(song, navigateBack = navigateBack, viewModel = viewModel)
                    1 -> QueueSection(viewModel)
                }
            }

            Spacer(Modifier.height(24.dp))

            PlayerControls(
                state = state, viewModel = viewModel,
                modifier = Modifier.fillMaxWidth()
            )

            FooterButtons(modifier = Modifier.padding(vertical = 16.dp), pagerState = pagerState)
        }
    }
}

@Composable
private fun SongScreen(song: Song, navigateBack: () -> Unit, viewModel: MusicPlayerViewModel) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        PlayerHeader(
            navigateBack, modifier = Modifier.fillMaxWidth()
        )

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
private fun PlayerHeader(navigateBack: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = { navigateBack() }) {
            Icon(
                imageVector = Icons.Filled.KeyboardArrowDown,
                contentDescription = "Close",
                tint = appColors.iconSecondary
            )
        }
        Text(
            text = "Playing from TODO:",
            style = MaterialTheme.typography.labelSmall,
            color = appColors.textMuted
        )
        IconButton(onClick = { }) {
            Icon(
                imageVector = Icons.Filled.MoreVert,
                contentDescription = "More",
                tint = appColors.iconSecondary
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
private fun PlayerControls(
    state: PlayerState,
    viewModel: MusicPlayerViewModel,
    modifier: Modifier = Modifier
) {
    var showFlowChip by remember { mutableStateOf(false) }
    var isFirstLaunch by remember { mutableStateOf(true) }

    LaunchedEffect(state.flowMode) {
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
            modifier = Modifier
        ) {
            IconButton(onClick = viewModel::changeShuffleOption) {
                Icon(
                    imageVector = Icons.Default.Shuffle,
                    contentDescription = "Shuffle",
                    tint = if (state.isShuffled) appColors.iconActive else appColors.iconSecondary
                )

                if (state.isShuffled) {
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

        IconButton(onClick = viewModel::onPreviousClicked) {
            Icon(
                imageVector = Icons.Filled.SkipPrevious,
                contentDescription = "Previous",
                tint = appColors.iconSecondary
            )
        }

        FilledIconButton(
            onClick = viewModel::onPlayPauseClicked,
            modifier = Modifier.size(64.dp),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = appColors.iconActive,
                contentColor = appColors.onAccent
            )
        ) {
            Icon(
                imageVector = if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = if (state.isPlaying) "Pause" else "Play",
                modifier = Modifier.size(32.dp)
            )
        }

        IconButton(onClick = viewModel::onNextClicked) {
            Icon(
                imageVector = Icons.Filled.SkipNext,
                contentDescription = "Next",
                tint = appColors.iconSecondary
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
                        label = { Text(state.flowMode.label, color = appColors.textPrimary, fontSize = 12.sp) },
                        colors = SuggestionChipDefaults.suggestionChipColors(containerColor = appColors.backgroundElevated),
                        border = null
                    )
                }
            }
            IconButton(onClick = viewModel::cycleFlowMode) {
                Icon(
                    imageVector = state.flowMode.icon,
                    contentDescription = "Flow",
                    tint = if (state.flowMode != FlowMode.STOP_AT_END) appColors.iconActive else appColors.iconSecondary
                )
                if (state.flowMode != FlowMode.STOP_AT_END) {
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

    LaunchedEffect(currentPosition) {
        if (!isSliding && duration > 0) {
            sliderPosition = currentPosition.toFloat() / duration.toFloat()
        }
    }

    Column(
        modifier = modifier.fillMaxWidth()
    ) {
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
                style = MaterialTheme.typography.bodySmall,
                color = appColors.textMuted
            )
            Text(
                text = formatTime(duration),
                style = MaterialTheme.typography.bodySmall,
                color = appColors.textMuted
            )
        }
    }
}

@Composable
private fun FooterButtons(
    modifier: Modifier = Modifier,
    pagerState: PagerState
) {
    val scope = rememberCoroutineScope()
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            IconButton(onClick = {
                scope.launch {
                    pagerState.animateScrollToPage(0)
                }
            }) {
                Icon(
                    imageVector = Icons.Filled.MusicNote,
                    contentDescription = "Song screen",
                    tint = if (pagerState.currentPage == 0) appColors.iconActive else appColors.iconSecondary
                )
            }
            Text("Now playing", color = appColors.textPrimary)
        }
        Spacer(modifier = Modifier.width(36.dp))
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            IconButton(
                onClick = {
                    scope.launch {
                        pagerState.animateScrollToPage(1)
                    }
                }
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                    contentDescription = "Queue",
                    tint = if (pagerState.currentPage == 1) appColors.iconActive else appColors.iconSecondary
                )
            }
            Text("Queue", color = appColors.textPrimary)
        }
    }
}
