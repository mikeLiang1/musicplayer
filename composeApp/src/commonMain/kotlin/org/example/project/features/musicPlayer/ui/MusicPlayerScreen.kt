package org.example.project.features.musicPlayer.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import org.example.project.core.manager.PlaybackMode
import org.example.project.features.musicPlayer.model.PlayerQueue
import org.example.project.ui.component.PlayPauseButton
import org.example.project.ui.theme.appColors

@Composable
fun MusicPlayerScreen(
    viewModel: MusicPlayerViewModel,
    navigateBack: () -> Unit
) {
    BackHandler { navigateBack() }

    val state by viewModel.playerState.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isShuffled by viewModel.isShuffled.collectAsStateWithLifecycle()
    val playbackMode by viewModel.playbackMode.collectAsStateWithLifecycle()
    val playerQueue by viewModel.playerQueue.collectAsStateWithLifecycle()
    val pagerState = rememberPagerState { 2 }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // ── Header ──────────────────────────────────────
        PlayerHeader(
            navigateBack = navigateBack,
            pagerState = pagerState,
            playerQueue = playerQueue,
            uiState = uiState,
            onHistoryClick = viewModel::onHistoryPillClicked,
            onEditQueueClicked = viewModel::onEditQueueClicked
        )

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f),
        ) { page ->
            when (page) {
                0 -> SongScreen(song = playerQueue.current, viewModel = viewModel)

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
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
            )

            FooterButtons(modifier = Modifier.padding(vertical = 16.dp), pagerState = pagerState)
        }

    }

}

@Composable
private fun PlayerHeader(
    modifier: Modifier = Modifier,
    navigateBack: () -> Unit,
    pagerState: PagerState,
    playerQueue: PlayerQueue,
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

        // center content swaps based on page
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.weight(1f)
        ) {
            if (pagerState.currentPage == 0) {
                Text(
                    text = "Playing from TODO:",
                    style = MaterialTheme.typography.labelSmall,
                    color = appColors.textMuted,
                    fontFamily = FontFamily.Monospace
                )
            } else {
                androidx.compose.animation.AnimatedVisibility(
                    visible = playerQueue.history.isNotEmpty(),
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
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
                                text = if (uiState.showHistory) "Hide history" else "${playerQueue.history.size} previous songs",
                                style = MaterialTheme.typography.labelSmall,
                                color = appColors.textMuted,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }
        if (pagerState.currentPage == 0) {
            IconButton(onClick = {}) {
                Icon(
                    imageVector = Icons.Filled.MoreVert,
                    contentDescription = "More",
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
            IconButton(onClick = viewModel::onPreviousClicked) {
                Icon(
                    imageVector = Icons.Filled.SkipPrevious,
                    contentDescription = "Previous",
                    tint = appColors.iconPrimary
                )
            }

            // Play/Pause button
            PlayPauseButton(viewModel::onPlayPauseClicked, isPlaying)

            // Next button
            IconButton(onClick = viewModel::onNextClicked) {
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
