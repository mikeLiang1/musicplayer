package org.example.project.features.musicPlayer.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.CancellationException
import org.example.project.ui.theme.appColors

@Composable
fun QueueSection(viewModel: MusicPlayerViewModel) {

    val playerState by viewModel.playerState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // 1. We manually track where the list should start visually.
    // Initialize it to the current index so it starts correctly.
    var visibleStartIndex by rememberSaveable { mutableIntStateOf(playerState.currentIndex + 1) }


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

    LazyColumn(state = listState, horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxSize()) {
        if (!uiState.showHistory && playerState.currentIndex > 0) {
            item {
                Surface(
                    onClick = {
                        viewModel.scrollWhenHistoryOpened()
                    },
                    color = appColors.backgroundElevated,
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
                            modifier = Modifier.size(14.dp),
                            tint = appColors.iconSecondary
                        )
                        Text(
                            text = "${playerState.currentIndex} previous songs",
                            style = MaterialTheme.typography.labelSmall,
                            color = appColors.textMuted
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
