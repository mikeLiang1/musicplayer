//package org.example.project.features.musicPlayer.ui
//
//import androidx.activity.compose.BackHandler
//import androidx.compose.animation.expandVertically
//import androidx.compose.animation.fadeIn
//import androidx.compose.animation.fadeOut
//import androidx.compose.animation.shrinkVertically
//import androidx.compose.foundation.BorderStroke
//import androidx.compose.foundation.background
//import androidx.compose.foundation.clickable
//import androidx.compose.foundation.layout.Arrangement
//import androidx.compose.foundation.layout.Box
//import androidx.compose.foundation.layout.Column
//import androidx.compose.foundation.layout.Row
//import androidx.compose.foundation.layout.Spacer
//import androidx.compose.foundation.layout.fillMaxSize
//import androidx.compose.foundation.layout.fillMaxWidth
//import androidx.compose.foundation.layout.height
//import androidx.compose.foundation.layout.navigationBarsPadding
//import androidx.compose.foundation.layout.offset
//import androidx.compose.foundation.layout.padding
//import androidx.compose.foundation.layout.size
//import androidx.compose.foundation.layout.statusBarsPadding
//import androidx.compose.foundation.layout.width
//import androidx.compose.foundation.pager.HorizontalPager
//import androidx.compose.foundation.pager.PagerState
//import androidx.compose.foundation.pager.rememberPagerState
//import androidx.compose.foundation.shape.CircleShape
//import androidx.compose.foundation.shape.RoundedCornerShape
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.automirrored.filled.QueueMusic
//import androidx.compose.material.icons.filled.Cancel
//import androidx.compose.material.icons.filled.Check
//import androidx.compose.material.icons.filled.Edit
//import androidx.compose.material.icons.filled.KeyboardArrowDown
//import androidx.compose.material.icons.filled.MoreVert
//import androidx.compose.material.icons.filled.MusicNote
//import androidx.compose.material.icons.filled.Pause
//import androidx.compose.material.icons.filled.PlayArrow
//import androidx.compose.material.icons.filled.Shuffle
//import androidx.compose.material.icons.filled.SkipNext
//import androidx.compose.material.icons.filled.SkipPrevious
//import androidx.compose.material.icons.rounded.ExpandLess
//import androidx.compose.material.icons.rounded.ExpandMore
//import androidx.compose.material3.FilledIconButton
//import androidx.compose.material3.HorizontalDivider
//import androidx.compose.material3.Icon
//import androidx.compose.material3.IconButton
//import androidx.compose.material3.IconButtonDefaults
//import androidx.compose.material3.MaterialTheme
//import androidx.compose.material3.Surface
//import androidx.compose.material3.Text
//import androidx.compose.runtime.Composable
//import androidx.compose.runtime.LaunchedEffect
//import androidx.compose.runtime.getValue
//import androidx.compose.runtime.mutableStateOf
//import androidx.compose.runtime.remember
//import androidx.compose.runtime.setValue
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.draw.clip
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.text.font.FontWeight
//import androidx.compose.ui.unit.dp
//import androidx.lifecycle.compose.collectAsStateWithLifecycle
//import org.example.project.core.model.FlowMode
//import org.example.project.ui.component.CoverImage
//import org.example.project.ui.theme.appColors
//
//@Composable
//fun MusicPlayerScreenRefactored(
//    viewModel: MusicPlayerViewModelRefactored,
//    navigateBack: () -> Unit
//) {
//    BackHandler { navigateBack() }
//
//    val state by viewModel.playerState.collectAsStateWithLifecycle()
//    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
//    val pagerState = rememberPagerState { 2 }
//
//    Surface(
//        modifier = Modifier.fillMaxSize(),
//        color = appColors.backgroundPrimary
//    ) {
//        Column(
//            modifier = Modifier
//                .fillMaxSize()
//                .statusBarsPadding()
//                .navigationBarsPadding()
//        ) {
//            // ── Header ──────────────────────────────────────
//            PlayerHeader(
//                navigateBack = navigateBack,
//                pagerState = pagerState,
//                playerState = state,
//                uiState = uiState,
//                onHistoryClick = viewModel::onHistoryPillClicked,
//                onEditQueueClicked = viewModel::onEditQueueClicked
//            )
//
//            HorizontalPager(
//                state = pagerState,
//                modifier = Modifier.weight(1f)
//            ) { page ->
//                when (page) {
//                    0 -> NowPlayingScreen(
//                        state = state,
//                        currentPosition = viewModel.currentPosition,
//                        onSeekTo = viewModel::onSeekTo,
//                        onPlayPauseClicked = viewModel::onPlayPauseClicked
//                    )
//                    1 -> QueueSectionRefactored(viewModel = viewModel)
//                }
//            }
//
//            // ── Controls ────────────────────────────────────
//            PlayerControlsRefactored(
//                state = state,
//                viewModel = viewModel,
//                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
//            )
//        }
//    }
//}
//
//@Composable
//private fun PlayerHeader(
//    navigateBack: () -> Unit,
//    pagerState: PagerState,
//    playerState: org.example.project.core.model.PlayerState,
//    uiState: MusicPlayerUiState,
//    onHistoryClick: () -> Unit,
//    onEditQueueClicked: () -> Unit,
//    modifier: Modifier = Modifier
//) {
//    Row(
//        modifier = modifier
//            .fillMaxWidth()
//            .padding(horizontal = 16.dp, vertical = 12.dp),
//        horizontalArrangement = Arrangement.SpaceBetween,
//        verticalAlignment = Alignment.CenterVertically
//    ) {
//        // Back button
//        IconButton(onClick = navigateBack) {
//            Icon(
//                imageVector = Icons.Filled.KeyboardArrowDown,
//                contentDescription = "Back",
//                tint = appColors.iconPrimary
//            )
//        }
//
//        // Page indicator
//        Row(
//            horizontalArrangement = Arrangement.spacedBy(8.dp),
//            verticalAlignment = Alignment.CenterVertically
//        ) {
//            PageIndicator(
//                label = "Now Playing",
//                isSelected = pagerState.currentPage == 0,
//                onClick = { /* pager scroll handled elsewhere */ }
//            )
//            PageIndicator(
//                label = "Queue",
//                isSelected = pagerState.currentPage == 1,
//                onClick = { /* pager scroll handled elsewhere */ }
//            )
//        }
//
//        // Queue edit button (only on queue page)
//        if (pagerState.currentPage == 1) {
//            IconButton(onClick = onEditQueueClicked) {
//                Icon(
//                    imageVector = if (uiState.isEditingQueue) Icons.Filled.Check else Icons.Filled.Edit,
//                    contentDescription = if (uiState.isEditingQueue) "Done editing" else "Edit queue",
//                    tint = if (uiState.isEditingQueue) appColors.accentPrimary else appColors.iconPrimary
//                )
//            }
//        } else {
//            // Placeholder to maintain layout
//            Box(modifier = Modifier.size(48.dp))
//        }
//    }
//
//    // History pill (only on queue page)
//    if (pagerState.currentPage == 1 && playerState.queue.size > 1) {
//        LaunchedEffect(uiState.showHistory) {
//            if (uiState.showHistory) {
//                // Scroll to current song when history opens
//                // This is handled in QueueScreen
//            }
//        }
//
//        HistoryPill(
//            showHistory = uiState.showHistory,
//            historyCount = playerState.queue.take(playerState.currentIndex).size,
//            onClick = onHistoryClick,
//            modifier = Modifier
//                .fillMaxWidth()
//                .padding(horizontal = 16.dp, vertical = 8.dp)
//        )
//    }
//}
//
//@Composable
//private fun PageIndicator(
//    label: String,
//    isSelected: Boolean,
//    onClick: () -> Unit,
//    modifier: Modifier = Modifier
//) {
//    Surface(
//        onClick = onClick,
//        shape = RoundedCornerShape(20.dp),
//        color = if (isSelected) appColors.accentContainer else Color.Transparent,
//        border = if (!isSelected) BorderStroke(1.dp, appColors.divider) else null,
//        modifier = modifier
//    ) {
//        Text(
//            text = label,
//            style = MaterialTheme.typography.labelMedium,
//            color = if (isSelected) appColors.accentPrimary else appColors.textMuted,
//            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
//        )
//    }
//}
//
//@Composable
//private fun HistoryPill(
//    showHistory: Boolean,
//    historyCount: Int,
//    onClick: () -> Unit,
//    modifier: Modifier = Modifier
//) {
//    Surface(
//        onClick = onClick,
//        shape = RoundedCornerShape(20.dp),
//        color = appColors.backgroundSecondary,
//        modifier = modifier
//    ) {
//        Row(
//            modifier = Modifier
//                .fillMaxWidth()
//                .padding(horizontal = 16.dp, vertical = 10.dp),
//            horizontalArrangement = Arrangement.SpaceBetween,
//            verticalAlignment = Alignment.CenterVertically
//        ) {
//            Text(
//                text = if (showHistory) "Hide history" else "Show history ($historyCount)",
//                style = MaterialTheme.typography.labelMedium,
//                color = appColors.textMuted
//            )
//            Icon(
//                imageVector = if (showHistory) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
//                contentDescription = null,
//                tint = appColors.iconSecondary,
//                modifier = Modifier.size(18.dp)
//            )
//        }
//    }
//}
//
//@Composable
//private fun NowPlayingScreen(
//    state: org.example.project.core.model.PlayerState,
//    currentPosition: androidx.compose.runtime.State<Long>,
//    onSeekTo: (Long) -> Unit,
//    onPlayPauseClicked: () -> Unit,
//    modifier: Modifier = Modifier
//) {
//    Column(
//        modifier = modifier.fillMaxSize(),
//        horizontalAlignment = Alignment.CenterHorizontally,
//        verticalArrangement = Arrangement.Center
//    ) {
//        // Album art
//        Box(
//            modifier = Modifier
//                .size(300.dp)
//                .clip(RoundedCornerShape(16.dp))
//                .background(appColors.backgroundSecondary)
//        ) {
//            state.currentSong?.let { song ->
//                CoverImage(
//                    data = song.thumbnailUrl,
//                    modifier = Modifier.fillMaxSize(),
//                    shape = RoundedCornerShape(16.dp)
//                )
//            } ?: run {
//                Icon(
//                    imageVector = Icons.Filled.MusicNote,
//                    contentDescription = "No song playing",
//                    tint = appColors.iconSecondary,
//                    modifier = Modifier
//                        .size(100.dp)
//                        .align(Alignment.Center)
//                )
//            }
//        }
//
//        Spacer(modifier = Modifier.height(32.dp))
//
//        // Song info
//        state.currentSong?.let { song ->
//            Text(
//                text = song.title,
//                style = MaterialTheme.typography.headlineMedium,
//                fontWeight = FontWeight.Bold,
//                color = appColors.textPrimary,
//                modifier = Modifier.padding(horizontal = 32.dp)
//            )
//            Spacer(modifier = Modifier.height(8.dp))
//            Text(
//                text = song.artist,
//                style = MaterialTheme.typography.bodyLarge,
//                color = appColors.textMuted,
//                modifier = Modifier.padding(horizontal = 32.dp)
//            )
//        }
//
//        Spacer(modifier = Modifier.height(32.dp))
//
//        // Progress bar would go here
//        // For now, just show a placeholder
//        Box(
//            modifier = Modifier
//                .fillMaxWidth()
//                .padding(horizontal = 32.dp)
//                .height(4.dp)
//                .background(appColors.divider)
//        )
//    }
//}
//
//@Composable
//private fun PlayerControlsRefactored(
//    state: org.example.project.core.model.PlayerState,
//    viewModel: MusicPlayerViewModelRefactored,
//    modifier: Modifier = Modifier
//) {
//    var showFlowChip by remember { mutableStateOf(false) }
//
//    Column(modifier = modifier) {
//        // Flow mode chip (conditional)
//        if (showFlowChip) {
//            androidx.compose.animation.AnimatedVisibility(
//                visible = showFlowChip,
//                enter = fadeIn() + expandVertically(),
//                exit = fadeOut() + shrinkVertically()
//            ) {
//                FlowModeChip(
//                    flowMode = state.flowMode,
//                    onDismiss = { showFlowChip = false },
//                    modifier = Modifier
//                        .fillMaxWidth()
//                        .padding(bottom = 12.dp)
//                )
//            }
//        }
//
//        Row(
//            modifier = Modifier.fillMaxWidth(),
//            horizontalArrangement = Arrangement.SpaceBetween,
//            verticalAlignment = Alignment.CenterVertically
//        ) {
//            // Shuffle button
//            Box(
//                contentAlignment = Alignment.Center,
//                modifier = Modifier
//            ) {
//                IconButton(onClick = viewModel::changeShuffleOption) {
//                    Icon(
//                        imageVector = Icons.Default.Shuffle,
//                        contentDescription = "Shuffle",
//                        tint = if (state.isShuffled) appColors.accentPrimary else appColors.iconSecondary
//                    )
//                }
//            }
//
//            // Previous button
//            IconButton(onClick = viewModel::onPreviousClicked) {
//                Icon(
//                    imageVector = Icons.Filled.SkipPrevious,
//                    contentDescription = "Previous",
//                    tint = appColors.iconPrimary
//                )
//            }
//
//            // Play/Pause button
//            FilledIconButton(
//                onClick = viewModel::onPlayPauseClicked,
//                modifier = Modifier.size(64.dp),
//                colors = IconButtonDefaults.filledIconButtonColors(
//                    containerColor = appColors.iconActive,
//                    contentColor = Color.White
//                )
//            ) {
//                Icon(
//                    imageVector = if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
//                    contentDescription = if (state.isPlaying) "Pause" else "Play",
//                    modifier = Modifier.size(32.dp)
//                )
//            }
//
//            // Next button
//            IconButton(onClick = viewModel::onNextClicked) {
//                Icon(
//                    imageVector = Icons.Filled.SkipNext,
//                    contentDescription = "Next",
//                    tint = appColors.iconPrimary
//                )
//            }
//
//            // Flow mode button
//            Box(
//                contentAlignment = Alignment.Center,
//                modifier = Modifier
//            ) {
//                IconButton(
//                    onClick = {
//                        viewModel.toggleRepeatMode()
//                        showFlowChip = true
//                    }
//                ) {
//                    Icon(
//                        imageVector = state.flowMode.icon,
//                        contentDescription = "Flow",
//                        tint = if (state.flowMode != FlowMode.STOP_AT_END) appColors.accentPrimary else appColors.iconSecondary
//                    )
//                }
//            }
//        }
//    }
//}
//
//@Composable
//private fun FlowModeChip(
//    flowMode: FlowMode,
//    onDismiss: () -> Unit,
//    modifier: Modifier = Modifier
//) {
//    Surface(
//        shape = RoundedCornerShape(20.dp),
//        color = appColors.accentContainer.copy(alpha = 0.3f),
//        border = BorderStroke(1.dp, appColors.accentContainer),
//        modifier = modifier
//    ) {
//        Row(
//            modifier = Modifier
//                .fillMaxWidth()
//                .padding(horizontal = 16.dp, vertical = 10.dp),
//            horizontalArrangement = Arrangement.SpaceBetween,
//            verticalAlignment = Alignment.CenterVertically
//        ) {
//            Text(
//                text = flowMode.label,
//                style = MaterialTheme.typography.labelMedium,
//                color = appColors.accentPrimary
//            )
//            IconButton(
//                onClick = onDismiss,
//                modifier = Modifier.size(24.dp)
//            ) {
//                Icon(
//                    imageVector = Icons.Filled.Cancel,
//                    contentDescription = "Dismiss",
//                    tint = appColors.iconSecondary,
//                    modifier = Modifier.size(16.dp)
//                )
//            }
//        }
//    }
//}
