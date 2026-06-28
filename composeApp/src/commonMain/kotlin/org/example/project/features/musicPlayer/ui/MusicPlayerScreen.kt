package org.example.project.features.musicPlayer.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LastPage
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import org.example.project.core.manager.PlaybackMode
import org.example.project.features.musicPlayer.model.PlayerQueue
import org.example.project.features.songMenu.ui.SongMenuAction
import org.example.project.features.songMenu.ui.rememberSongMenuController
import org.example.project.ui.component.PlayPauseButton
import org.example.project.ui.theme.appColors

@Composable
fun MusicPlayerScreen(
    viewModel: MusicPlayerViewModel,
    onDismissRequest: () -> Unit
) {
    BackHandler { onDismissRequest() }

    val state by viewModel.playerState.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isShuffled by viewModel.isShuffled.collectAsStateWithLifecycle()
    val playbackMode by viewModel.playbackMode.collectAsStateWithLifecycle()
    val displayQueue by viewModel.displayQueue.collectAsStateWithLifecycle()
    val pagerState = rememberPagerState { 2 }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // ── Header (close · drag handle · menu) ─────────
        PlayerHeader(
            navigateBack = onDismissRequest,
            pagerState = pagerState,
            displayQueue = displayQueue,
            uiState = uiState,
            onHistoryClick = viewModel::onHistoryPillClicked,
            onEditQueueClicked = viewModel::onEditQueueClicked
        )

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f),
        ) { page ->
            when (page) {
                0 -> SongScreen(song = displayQueue.current, viewModel = viewModel)

                1 -> QueueScreen(viewModel = viewModel)
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            HorizontalDivider(
                color = appColors.dividerSubtle,
                thickness = 1.dp
            )
            Spacer(modifier = Modifier.height(16.dp))

            PlayerControls(
                isPlaying = state.isPlaying,
                isShuffled = isShuffled,
                playbackMode = playbackMode,
                viewModel = viewModel,
                onPrevious = viewModel::onPreviousClicked,
                onNext = viewModel::onNextClicked,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
            )

            FooterButtons(modifier = Modifier.padding(vertical = 16.dp), pagerState = pagerState)
        }

    }

}

@Composable
private fun DragHandle() {
    Box(
        modifier = Modifier
            .width(36.dp)
            .height(4.dp)
            .clip(RoundedCornerShape(99.dp))
            .background(appColors.dividerSubtle)
    )
}

@Composable
private fun PlayerHeader(
    modifier: Modifier = Modifier,
    navigateBack: () -> Unit,
    pagerState: PagerState,
    displayQueue: PlayerQueue,
    uiState: MusicPlayerUiState,
    onHistoryClick: () -> Unit,
    onEditQueueClicked: () -> Unit
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = navigateBack) {
            Icon(
                imageVector = Icons.Filled.KeyboardArrowDown,
                contentDescription = "Close",
                tint = appColors.iconSecondary
            )
        }

        // center content: drag handle, or the history pill on the queue page
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.weight(1f)
        ) {
            if (pagerState.currentPage == 1 && displayQueue.history.isNotEmpty()) {
                Surface(
                    onClick = onHistoryClick, // viewModel handles toggle logic
                    color = appColors.backgroundElevated,
                    shape = RoundedCornerShape(99.dp),
                    border = BorderStroke(1.dp, appColors.divider)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (uiState.showHistory) Icons.Rounded.ExpandMore else Icons.Rounded.ExpandLess,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = appColors.iconMuted
                        )
                        Text(
                            text = if (uiState.showHistory) "Hide history" else "${displayQueue.history.size} previous songs",
                            style = MaterialTheme.typography.labelSmall,
                            color = appColors.textMuted,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            } else {
                DragHandle()
            }
        }
        val songMenu = rememberSongMenuController(
            listOf(
                SongMenuAction.AddToQueue,
                SongMenuAction.AddToPlaylist,
                SongMenuAction.GoToArtist,
                SongMenuAction.GoToAlbum
            )
        )

        if (pagerState.currentPage == 0) {
            IconButton(onClick = {
                displayQueue.current?.let {
                    songMenu.show(it)
                }
            }) {
                Icon(
                    imageVector = Icons.Filled.MoreVert,
                    contentDescription = "Menu",
                    tint = appColors.iconSecondary
                )
            }
        } else {
            IconButton(onClick = onEditQueueClicked) {
                Icon(
                    imageVector = if (!uiState.isEditingQueue) Icons.Filled.Edit else Icons.Filled.Check,
                    contentDescription = "Edit queue",
                    tint = appColors.iconSecondary
                )
            }
        }
    }
}

@Composable
private fun PlayerControls(
    isPlaying: Boolean,
    isShuffled: Boolean,
    playbackMode: PlaybackMode,
    viewModel: MusicPlayerViewModel,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {

    Column(modifier = modifier) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Shuffle button
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
            ) {
                IconButton(onClick = viewModel::changeShuffleOption) {
                    Icon(
                        imageVector = Icons.Default.Shuffle,
                        contentDescription = "Shuffle",
                        tint = if (isShuffled) appColors.accentPrimary else appColors.iconSecondary
                    )
                }
            }

            // Previous button
            IconButton(onClick = onPrevious) {
                Icon(
                    imageVector = Icons.Filled.SkipPrevious,
                    contentDescription = "Previous",
                    tint = appColors.iconPrimary
                )
            }

            // Play/Pause button
            PlayPauseButton(viewModel::onPlayPauseClicked, isPlaying)

            // Next button
            IconButton(onClick = onNext) {
                Icon(
                    imageVector = Icons.Filled.SkipNext,
                    contentDescription = "Next",
                    tint = appColors.iconPrimary
                )
            }

            // Repeat mode button
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
            ) {
                IconButton(
                    onClick = {
                        viewModel.togglePlaybackMode()
                    }
                ) {
                    Icon(
                        imageVector = when (playbackMode) {
                            PlaybackMode.OFF -> Icons.AutoMirrored.Filled.LastPage
                            PlaybackMode.REPEAT -> Icons.Rounded.Repeat
                            PlaybackMode.Infinite -> Icons.Default.AllInclusive
                        },
                        contentDescription = "Repeat",
                        tint = if (playbackMode != PlaybackMode.OFF) appColors.accentPrimary else appColors.iconSecondary
                    )
                }
            }
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
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.clickable {
                scope.launch { pagerState.animateScrollToPage(0) }
            }) {

            Icon(
                imageVector = Icons.Filled.MusicNote,
                contentDescription = "Song screen",
                tint = if (pagerState.currentPage == 0) appColors.iconActive else appColors.iconSecondary
            )

            Text(
                "Now playing",
                style = MaterialTheme.typography.labelSmall,
                color = if (pagerState.currentPage == 0) appColors.accentPrimary else appColors.textMuted
            )
        }

        Spacer(modifier = Modifier.width(36.dp))

        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable {
            scope.launch { pagerState.animateScrollToPage(1) }
        }) {

            Icon(
                imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                contentDescription = "Queue",
                tint = if (pagerState.currentPage == 1) appColors.iconActive else appColors.iconSecondary
            )

            Text(
                "Queue",
                style = MaterialTheme.typography.labelSmall,
                color = if (pagerState.currentPage == 1) appColors.accentPrimary else appColors.textMuted
            )
        }
    }
}
