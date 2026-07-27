package org.example.project.features.musicPlayer.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import org.example.project.core.manager.PlaybackMode
import org.example.project.core.model.QueueContext
import org.example.project.core.model.QueueContextType
import org.example.project.features.musicPlayer.model.PlayerQueue
import org.example.project.features.songMenu.ui.SongMenuAction
import org.example.project.features.songMenu.ui.SongMenuController
import org.example.project.features.songMenu.ui.SongMenuEffect
import org.example.project.features.songMenu.ui.rememberSongMenuController
import org.example.project.ui.component.PlayPauseButton
import org.example.project.ui.theme.Dimens
import org.example.project.ui.theme.appColors

@Composable
fun MusicPlayerScreen(
    viewModel: MusicPlayerViewModel,
    onDismissRequest: () -> Unit
) {
    BackHandler { onDismissRequest() }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val displayQueue by viewModel.displayQueue.collectAsStateWithLifecycle()
    val pagerState = rememberPagerState { 2 }

    val songMenu = rememberSongMenuController()
    var isSleepTimerSheetVisible by remember { mutableStateOf(false) }

    // The sleep timer lives in the ⋮ menu (a song-scoped sheet), but its own sheet belongs here.
    LaunchedEffect(songMenu) {
        songMenu.effect.collect { effect ->
            when (effect) {
                SongMenuEffect.OpenSleepTimer -> isSleepTimerSheetVisible = true
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // ── Drag handle (own row, above the header) ─────
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = Dimens.spaceS)
        ) {
            DragHandle()
        }

        // ── Header (close · playing-from · menu) ────────
        PlayerHeader(
            navigateBack = onDismissRequest,
            pagerState = pagerState,
            displayQueue = displayQueue,
            uiState = uiState,
            songMenu = songMenu,
            onHistoryClick = viewModel::onHistoryPillClicked,
            onEditQueueClicked = viewModel::onEditQueueClicked
        )

        SleepTimerBottomSheet(
            isVisible = isSleepTimerSheetVisible,
            sleepTimerEndAtMs = uiState.sleepTimerEndAtMs,
            sleepTimerEndOfTrack = uiState.sleepTimerEndOfTrack,
            onDismissRequest = { isSleepTimerSheetVisible = false },
            onDurationSelected = viewModel::setSleepTimer,
            onEndOfTrackSelected = viewModel::setSleepTimerEndOfTrack,
            onCancelTimer = viewModel::cancelSleepTimer
        )

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f),
        ) { page ->
            when (page) {
                0 -> SongScreen(
                    song = displayQueue.current,
                    viewModel = viewModel,
                    songMenu = songMenu
                )

                1 -> QueueScreen(
                    viewModel = viewModel,
                    songMenu = songMenu,
                    isActivePage = pagerState.currentPage == 1
                )
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            HorizontalDivider(
                color = appColors.dividerSubtle,
                thickness = Dimens.strokeThin
            )
            Spacer(modifier = Modifier.height(Dimens.spaceL))

            PlayerControls(
                viewModel = viewModel,
                modifier = Modifier.padding(horizontal = Dimens.spaceXl, vertical = Dimens.spaceL)
            )

            FooterButtons(modifier = Modifier.padding(vertical = Dimens.spaceL), pagerState = pagerState)
        }

    }

}

@Composable
private fun DragHandle() {
    Box(
        modifier = Modifier
            .width(Dimens.Size.dragHandleWidth)
            .height(Dimens.spaceXs)
            .clip(RoundedCornerShape(Dimens.Size.pillWidth))
            .background(appColors.dividerSubtle)
    )
}

/**
 * "PLAYING FROM PLAYLIST / Late Night Drive" — the queue's source, centered in the header.
 * Two lines: the source kind in muted caps, then its name (marquee'd when it doesn't fit
 * the narrow space left between the close button and the trailing actions).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun QueueContextLabel(
    context: QueueContext,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = when (context.type) {
                QueueContextType.PLAYLIST -> "PLAYING FROM PLAYLIST"
                QueueContextType.LIKED_SONGS -> "PLAYING FROM"
                QueueContextType.RADIO -> "PLAYING FROM SONG RADIO"
            },
            style = MaterialTheme.typography.labelSmall,
            color = appColors.textMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = context.title,
            style = MaterialTheme.typography.labelLarge,
            color = appColors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.basicMarquee()
        )
    }
}

@Composable
private fun PlayerHeader(
    modifier: Modifier = Modifier,
    navigateBack: () -> Unit,
    pagerState: PagerState,
    displayQueue: PlayerQueue,
    uiState: MusicPlayerUiState,
    songMenu: SongMenuController,
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

        // center content: the queue's source, or the history pill on the queue page
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.weight(1f)
        ) {
            if (pagerState.currentPage == 1 && displayQueue.history.isNotEmpty()) {
                Surface(
                    onClick = onHistoryClick, // viewModel handles toggle logic
                    color = appColors.backgroundElevated,
                    shape = RoundedCornerShape(Dimens.Size.pillWidth),
                    border = BorderStroke(Dimens.strokeThin, appColors.divider)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = Dimens.spaceL, vertical = Dimens.spaceXs),
                        horizontalArrangement = Arrangement.spacedBy(Dimens.spaceXs),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (uiState.showHistory) Icons.Rounded.ExpandMore else Icons.Rounded.ExpandLess,
                            contentDescription = null,
                            modifier = Modifier.size(Dimens.iconL),
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
                uiState.queueContext?.let { QueueContextLabel(context = it) }
            }
        }
        // Exactly one trailing button on either page, matching the single leading close button —
        // that symmetry is what keeps the center content centered on the bar.
        val isSleepTimerActive = uiState.sleepTimerEndAtMs != null || uiState.sleepTimerEndOfTrack
        if (pagerState.currentPage == 0) {
            IconButton(onClick = {
                displayQueue.current?.let {
                    songMenu.show(
                        it,
                        listOf(
                            SongMenuAction.AddToQueue,
                            SongMenuAction.AddToPlaylist,
                            SongMenuAction.GoToArtist,
                            SongMenuAction.GoToAlbum,
                            SongMenuAction.SleepTimer
                        )
                    )
                }
            }) {
                Icon(
                    imageVector = Icons.Filled.MoreVert,
                    contentDescription = "Menu",
                    // An armed timer tints the menu that now hides it, so it stays discoverable.
                    tint = if (isSleepTimerActive) appColors.accentPrimary else appColors.iconSecondary
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
    viewModel: MusicPlayerViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.playerState.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isPlaying = state.isPlaying
    val isBuffering = state.isBuffering
    val willPlayWhenReady = state.playWhenReady
    val isShuffled = uiState.isShuffled
    val playbackMode = uiState.playbackMode

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
            PlayPauseButton(
                onPressed = viewModel::onPlayPauseClicked,
                isPlaying = isPlaying,
                isBuffering = isBuffering,
                willPlayWhenReady = willPlayWhenReady
            )

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

        Spacer(modifier = Modifier.width(Dimens.Size.dragHandleWidth))

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
