package org.example.project.features.musicPlayer.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.example.project.core.model.FlowMode
import org.example.project.core.model.PlayerState
import org.example.project.ui.theme.appColors

@Composable
fun MusicPlayerScreen(
    viewModel: MusicPlayerViewModel,
    navigateBack: () -> Unit
) {
    BackHandler { navigateBack() }

    val state by viewModel.playerState.collectAsStateWithLifecycle()
    val pagerState = rememberPagerState(pageCount = { 2 })

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()


    state.currentSong?.let { song ->
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()

        ) {
            PlayerHeader(
                navigateBack = navigateBack,
                pagerState = pagerState,
                playerState = state,
                showHistory = uiState.showHistory,
                onHistoryClick = viewModel::onHistoryPillClicked
            )

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f),
                userScrollEnabled = false
            ) { page ->
                when (page) {
                    0 -> SongScreen(song, viewModel = viewModel)
                    1 -> QueueSection(viewModel)
                }
            }


            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                HorizontalDivider(
                    color = appColors.dividerSubtle,
                    thickness = 1.dp
                )
                Spacer(modifier = Modifier.height(16.dp))

                PlayerControls(
                    state = state, viewModel = viewModel,
                    modifier = Modifier.fillMaxWidth()
                )

                FooterButtons(modifier = Modifier.padding(vertical = 16.dp), pagerState = pagerState)
            }
        }
    }
}

@Composable
private fun PlayerHeader(
    navigateBack: () -> Unit,
    pagerState: PagerState,
    playerState: PlayerState,
    onHistoryClick: () -> Unit,
    showHistory: Boolean,
    modifier: Modifier = Modifier
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
                    visible = playerState.currentIndex > 0,
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
                                imageVector = if (showHistory) Icons.Rounded.ExpandMore else Icons.Rounded.ExpandLess,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = appColors.iconMuted
                            )
                            Text(
                                text = if (showHistory) "Hide history" else "${playerState.currentIndex} previous songs",
                                style = MaterialTheme.typography.labelSmall,
                                color = appColors.textMuted,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }

        IconButton(onClick = {}) {
            Icon(
                imageVector = Icons.Filled.MoreVert,
                contentDescription = "More",
                tint = appColors.iconSecondary
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
