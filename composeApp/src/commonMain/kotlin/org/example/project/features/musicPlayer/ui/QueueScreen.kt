package org.example.project.features.musicPlayer.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.CancellationException
import org.example.project.core.manager.PlaybackMode
import org.example.project.features.songMenu.ui.AddToPlaylistBottomSheet
import org.example.project.features.songMenu.ui.SongMenuBottomSheet
import org.example.project.ui.component.SongItem
import org.example.project.ui.component.SongItemState
import org.example.project.ui.theme.appColors
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueScreen(viewModel: MusicPlayerViewModel) {

    val playerState by viewModel.playerState.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val displayQueue by viewModel.displayQueue.collectAsStateWithLifecycle()
    val playbackMode by viewModel.playbackMode.collectAsStateWithLifecycle()
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()

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
            listState.requestScrollToItem(index)
            try {
                listState.animateScrollToItem(index)
            } catch (e: CancellationException) {
            }
        }
    }


    SongMenuBottomSheet(
        isMenuBottomSheetVisible = uiState.isMenuBottomSheetVisible,
        onCloseBottomSheet = {
            viewModel.onCloseMenuBottomSheet()
        },
        handleBottomSheetAction = { viewModel.handleBottomSheetAction(it) },
        songMenuActions = listOf()
    )

    AddToPlaylistBottomSheet(
        isBottomSheetVisible = uiState.isAddToPlaylistBottomSheetVisible,
        onCloseBottomSheet = viewModel::onClosePlaylistBottomSheet,
        playlists = playlists,
        onPlaylistClicked = viewModel::addSongToSelectedPlaylist
    )

    Column(modifier = Modifier.fillMaxSize()) {

        // ── Pinned current song ──────────────────────────
        displayQueue.current?.let { currentSong ->
            SongItem(
                song = currentSong,
                state = SongItemState.Current(playerState.isPlaying),
                onClick = { viewModel.onMenuClicked(currentSong) },
                onMenuClicked = { viewModel.onMenuClicked(currentSong) })
        }

        // ── Scrollable queue ─────────────────────────────
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(bottom = 8.dp)
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
                            onMenuClicked = { viewModel.onMenuClicked(song) },
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
                            state = SongItemState.Current(playerState.isPlaying),
                            dragHandleModifier = Modifier,
                            onMenuClicked = { viewModel.onMenuClicked(currentSong) }
                        ) { /* can't tap current to change, already playing */ }
                    }
                }
            }

            val combinedQueue = displayQueue.manual + displayQueue.upcoming
            val manualCount = displayQueue.manual.size

            if (combinedQueue.isNotEmpty()) {

                if (manualCount > 0) {
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
                                onMenuClicked = { viewModel.onMenuClicked(song) },
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
                                onMenuClicked = { viewModel.onMenuClicked(song) },
                                onRemoveClicked = { viewModel.removeSong(song.uniqueId) }
                            ) { viewModel.changePlayingToSong(song.uniqueId) }
                        }
                    }
                }
            }

            item(key = "footer_repeat_mode") {
                PlaybackModeFooter(
                    playbackMode = playbackMode,
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
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
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

//@Composable
//private fun CurrentSongRow(song: Song, isPlaying: Boolean) {
//    Row(
//        modifier = Modifier
//            .fillMaxWidth()
//            .background(appColors.accentContainer.copy(alpha = 0.3f))
//            .padding(horizontal = 16.dp, vertical = 10.dp),
//        verticalAlignment = Alignment.CenterVertically,
//        horizontalArrangement = Arrangement.spacedBy(12.dp)
//    ) {
//        // Artwork with eq overlay
//        Box(modifier = Modifier.size(48.dp)) {
//            CoverImage(
//                data = song.thumbnailUrl,
//                modifier = Modifier.fillMaxSize(),
//                shape = RoundedCornerShape(8.dp)
//            )
//            Box(
//                modifier = Modifier
//                    .fillMaxSize()
//                    .clip(RoundedCornerShape(8.dp))
//                    .background(Color.Black.copy(alpha = 0.45f)),
//                contentAlignment = Alignment.Center
//            ) {
//                EqualizerBars(isPlaying)
//            }
//        }
//
//        // Left accent bar
//        Box(
//            modifier = Modifier
//                .width(3.dp)
//                .height(48.dp)
//                .clip(RoundedCornerShape(99.dp))
//                .background(appColors.accentPrimary)
//        )
//
//        Column(modifier = Modifier.weight(1f)) {
//            Text(
//                text = song.title,
//                style = MaterialTheme.typography.bodyMedium,
//                fontWeight = FontWeight.SemiBold,
//                color = appColors.accentPrimary,
//                maxLines = 1,
//                overflow = TextOverflow.Ellipsis
//            )
//            Text(
//                text = "${song.artist} • ${formatTime(song.duration)}",
//                style = MaterialTheme.typography.bodySmall,
//                color = appColors.textMuted
//            )
//        }
//        IconButton(
//            onClick = { ) }
//        ) {
//            Icon(
//                imageVector = Icons.Filled.MoreVert,
//                contentDescription = "Song menu",
//                tint = appColors.iconSecondary
//            )
//        }
//    }
//}

@Composable
private fun EqualizerBars(isPlaying: Boolean) {
    val bars = listOf(0.3f, 0.7f, 1f, 0.5f)
    val lifecycle = LocalLifecycleOwner.current

    Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom,
        modifier = Modifier.height(20.dp)
    ) {
        bars.forEachIndexed { i, base ->
            val scale = remember { Animatable(base * 0.3f) }

            LaunchedEffect(isPlaying) {
                lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    if (isPlaying) {
                        while (true) {
                            scale.animateTo(
                                base,
                                animationSpec = tween(
                                    durationMillis = 500 + i * 80,
                                    easing = FastOutSlowInEasing
                                )
                            )
                            scale.animateTo(
                                base * 0.3f,
                                animationSpec = tween(
                                    durationMillis = 500 + i * 80,
                                    easing = FastOutSlowInEasing
                                )
                            )
                        }
                    } else {
                        scale.stop()
                    }
                }
            }

            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(20.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .graphicsLayer {
                        scaleY = scale.value
                        transformOrigin = TransformOrigin(0.5f, 1f)
                    }
                    .background(appColors.accentPrimary)
            )
        }
    }
}

@Composable
private fun PlaybackModeFooter(playbackMode: PlaybackMode, onPlaybackModeClicked: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = { onPlaybackModeClicked() })
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
                imageVector = when (playbackMode) {
                    PlaybackMode.OFF -> Icons.Rounded.Stop
                    PlaybackMode.REPEAT -> Icons.Filled.Repeat
                    PlaybackMode.Infinite -> Icons.Filled.AllInclusive
                },
                contentDescription = null,
                tint = appColors.accentPrimary,
                modifier = Modifier.size(14.dp)
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
