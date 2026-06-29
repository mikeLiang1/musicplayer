package org.example.project.features.musicPlayer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.CancellationException
import org.example.project.core.manager.PlaybackMode
import org.example.project.features.songMenu.ui.SongMenuAction
import org.example.project.features.songMenu.ui.rememberSongMenuController
import org.example.project.ui.component.SongItem
import org.example.project.ui.component.SongItemState
import org.example.project.ui.theme.Dimens
import org.example.project.ui.theme.appColors
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueScreen(viewModel: MusicPlayerViewModel, isActivePage: Boolean = true) {

    val playerState by viewModel.playerState.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val displayQueue by viewModel.displayQueue.collectAsStateWithLifecycle()

    val reorderableState = rememberReorderableLazyListState(listState) { from, to ->
        viewModel.onMove(from.key as String, to.key as String)
    }


    LaunchedEffect(reorderableState.isAnyItemDragging) {
        if (!reorderableState.isAnyItemDragging) {
            viewModel.onDragEnd()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                MusicPlayerEffect.ScrollUpWhenHistoryOpened -> {
                    // For dual-queue, we need to find the index in the displayed list
                    val currentSong = displayQueue.current
                    if (currentSong != null) {
                        // Find index of current song in the combined list
                        val allSongs =
                            displayQueue.history + listOfNotNull(currentSong) + displayQueue.manual + displayQueue.upcoming
                        val currentIndex = allSongs.indexOf(currentSong)
                        listState.requestScrollToItem(currentIndex)
                        val targetIndex = (currentIndex - 2).coerceAtLeast(0)
                        val itemHeight = listState.layoutInfo.visibleItemsInfo.firstOrNull()?.size ?: 0
                        try {
                            listState.animateScrollToItem(targetIndex, -itemHeight / 2)
                        } catch (e: CancellationException) {
                        }
                    }
                }

                MusicPlayerEffect.ScrollToFirst -> {
                    listState.animateScrollToItem(0)
                }
            }
        }
    }

    // Scroll when track changes
    LaunchedEffect(displayQueue.current) {
        if (displayQueue.current != null) {
            val allSongs =
                displayQueue.history + listOfNotNull(displayQueue.current) + displayQueue.manual + displayQueue.upcoming
            val currentIndex = allSongs.indexOf(displayQueue.current)
            val index = if (uiState.showHistory) currentIndex else 0
            // Instant reposition always (cheap, no frames) so the list is correct when opened;
            // only animate when the page is actually visible to avoid scrolling an offscreen list.
            listState.requestScrollToItem(index)
            if (isActivePage) {
                try {
                    listState.animateScrollToItem(index)
                } catch (e: CancellationException) {
                }
            }
        }
    }

    val songMenu = rememberSongMenuController(
        listOf(
            SongMenuAction.AddToQueue,
            SongMenuAction.AddToPlaylist,
            SongMenuAction.GoToArtist,
            SongMenuAction.GoToAlbum,
            SongMenuAction.RemoveFromQueue
        )
    )

    Column(modifier = Modifier.fillMaxSize()) {

        // ── Pinned current song ──────────────────────────
        displayQueue.current?.let { currentSong ->
            SongItem(
                song = currentSong,
                state = SongItemState.Current(playerState.isPlaying && isActivePage),
                onClick = { },
                onMenuClicked = { songMenu.show(currentSong) })
        }

        // ── Scrollable queue ─────────────────────────────
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(bottom = Dimens.spaceS)
        ) {

            // ── History (only when showHistory = true) ───
            if (uiState.showHistory && displayQueue.history.isNotEmpty()) {
                stickyHeader(key = "header_history") {
                    SectionDivider(label = "History")
                }
                items(displayQueue.history, key = { it.uniqueId }) { song ->
                    ReorderableItem(reorderableState, key = song.uniqueId) {
                        SongItem(
                            modifier = Modifier.animateItem(),
                            song = song,
                            state = SongItemState.Previous,
                            dragHandleModifier = Modifier,
                            onMenuClicked = { songMenu.show(song) },
                        ) { viewModel.changePlayingToSong(song.uniqueId) }
                    }
                }

                // Current song inline — only when history is open
                displayQueue.current?.let { currentSong ->
                    stickyHeader(key = "header_current") {
                        SectionDivider(label = "Now Playing")
                    }
                    item(key = currentSong.uniqueId) {
                        SongItem(
                            song = currentSong,
                            state = SongItemState.Current(playerState.isPlaying && isActivePage),
                            dragHandleModifier = Modifier,
                            onMenuClicked = { songMenu.show(currentSong) }
                        ) { /* can't tap current to change, already playing */ }
                    }
                }
            }

            if (displayQueue.manual.isNotEmpty() || displayQueue.upcoming.isNotEmpty()) {

                if (displayQueue.manual.isNotEmpty()) {
                    stickyHeader(key = "header_manual") {
                        SectionDivider(label = "Playing Next")
                    }
                    items(displayQueue.manual, key = { it.uniqueId }) { song ->
                        ReorderableItem(reorderableState, key = song.uniqueId) {
                            SongItem(
                                modifier = Modifier.animateItem(),
                                song = song,
                                state = SongItemState.Manual,
                                isEditMode = uiState.isEditingQueue,
                                dragHandleModifier = Modifier.draggableHandle(),
                                onMenuClicked = { songMenu.show(song) },
                                onRemoveClicked = { viewModel.removeSong(song.uniqueId) }
                            ) { viewModel.changePlayingToSong(song.uniqueId) }
                        }
                    }
                }

                if (displayQueue.upcoming.isNotEmpty()) {
                    stickyHeader(key = "header_upcoming") {
                        SectionDivider(label = "Queue")
                    }
                    items(displayQueue.upcoming, key = { it.uniqueId }) { song ->
                        ReorderableItem(reorderableState, key = song.uniqueId) {
                            SongItem(
                                modifier = Modifier.animateItem(),
                                song = song,
                                isEditMode = uiState.isEditingQueue,
                                dragHandleModifier = Modifier.draggableHandle(),
                                onMenuClicked = { songMenu.show(song) },
                                onRemoveClicked = { viewModel.removeSong(song.uniqueId) }
                            ) { viewModel.changePlayingToSong(song.uniqueId) }
                        }
                    }
                }
            }

            item(key = "footer_repeat_mode") {
                PlaybackModeFooter(
                    playbackMode = uiState.playbackMode,
                    onPlaybackModeClicked = { viewModel.togglePlaybackMode() })
            }
        }
    }
}

@Composable
private fun SectionDivider(label: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(appColors.backgroundSecondary) // solid bg so scrolling content doesn't bleed through
            .padding(horizontal = Dimens.spaceL, vertical = Dimens.spaceXs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.spaceM)
    ) {
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = appColors.divider
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = appColors.textMuted,
            fontFamily = FontFamily.Monospace
        )
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = appColors.divider
        )
    }
}


@Composable
private fun PlaybackModeFooter(playbackMode: PlaybackMode, onPlaybackModeClicked: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = { onPlaybackModeClicked() })
            .padding(horizontal = Dimens.spaceXl, vertical = Dimens.spaceM),
        horizontalArrangement = Arrangement.spacedBy(Dimens.spaceM),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(Dimens.spaceXxl)
                .clip(RoundedCornerShape(Dimens.radiusM))
                .background(appColors.accentContainer.copy(alpha = 0.3f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = when (playbackMode) {
                    PlaybackMode.OFF -> Icons.Rounded.Stop
                    PlaybackMode.REPEAT -> Icons.Filled.Repeat
                    PlaybackMode.Infinite -> Icons.Filled.AllInclusive
                },
                contentDescription = null,
                tint = appColors.accentPrimary,
                modifier = Modifier.size(Dimens.spaceM)
            )
        }
        Column {
            Text(
                text = when (playbackMode) {
                    PlaybackMode.OFF -> "Stop at end"
                    PlaybackMode.REPEAT -> "Repeat playlist"
                    PlaybackMode.Infinite -> "Infinite queue"
                },
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = appColors.textMuted
            )
            Text(
                text = when (playbackMode) {
                    PlaybackMode.OFF -> "Playback stops after last song"
                    PlaybackMode.REPEAT -> "Will loop back to start"
                    PlaybackMode.Infinite -> "Finding similar songs…"
                },
                style = MaterialTheme.typography.labelSmall,
                color = appColors.textDim
            )
        }
    }
}
