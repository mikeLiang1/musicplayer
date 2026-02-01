package org.example.project.features.search.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import kotlinx.coroutines.flow.distinctUntilChanged
import org.example.project.core.model.Song

@Composable
fun SearchScreen(searchViewModel: SearchViewModel) {
    val state by searchViewModel.uiState.collectAsStateWithLifecycle()

//    LaunchedEffect(Unit) {
//        searchViewModel.effect.collect { effect ->
//            when (effect) {
//                is SearchEffect.NavigateToResult -> {
//                    navigateToResult()
//                }
//            }
//        }
//    }

    BackHandler(enabled = !state.onSearchScreen) { searchViewModel.onBackPressed() }

    val focusManager = LocalFocusManager.current

    val interactionSource = remember { MutableInteractionSource() }

// This block listens for a "Release" event, which is effectively a click
    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { interaction ->
            if (interaction is PressInteraction.Release) {
                searchViewModel.onBackPressed()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .padding(top = 16.dp)
    ) {
        // Search Bar
        TextField(
            value = state.searchQuery,
            onValueChange = { searchViewModel.onQueryChanged(it) },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search on YouTube Music...") },
            trailingIcon = {
                IconButton(onClick = {
                    searchViewModel.onSuggestionClicked(state.searchQuery)
                    focusManager.clearFocus()
                }) {
                    Icon(Icons.Default.Search, contentDescription = null)
                }
            },
            interactionSource = interactionSource,
            keyboardActions = KeyboardActions {
                searchViewModel.onSuggestionClicked(state.searchQuery)
                focusManager.clearFocus()
            }
        )

        val listState = rememberLazyListState()

        // PAGINATION LOGIC OUTSIDE AnimatedContent
        LaunchedEffect(listState) {
            snapshotFlow {
                val lastVisibleItem = listState.layoutInfo.visibleItemsInfo.lastOrNull()
                lastVisibleItem != null &&
                        lastVisibleItem.index >= listState.layoutInfo.totalItemsCount - 5
            }
                .distinctUntilChanged()
                .collect { shouldLoadMore ->
                    if (shouldLoadMore && !state.isLoadingMore && !state.onSearchScreen) {
                        searchViewModel.searchMoreSongs()
                    }
                }
        }

        // Results List
        AnimatedContent(
            targetState = state.onSearchScreen,
            transitionSpec = {
                fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
            }
        ) { onSearchScreen ->
            LazyColumn(
                state= listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (onSearchScreen) {
                    // --- SEARCH SUGGESTIONS SCREEN ---
                    items(state.suggestions) { suggestion ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    searchViewModel.onSuggestionClicked(suggestion)
                                    focusManager.clearFocus()
                                }
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(suggestion)
                        }
                    }

                } else {
                    // --- RESULTS SCREEN ---
                    if (state.isLoading) {
                        item {
                            Box(
                                modifier = Modifier.fillParentMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                    }
                    items(state.songList) { song ->
                        SongItem(song) { }
                    }
                    if (state.isLoadingMore) {
                        item {
                            CircularProgressIndicator(modifier = Modifier.padding(8.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SongItem(song: Song, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage( // From Coil library
            model = song.thumbnailUrl,
            contentDescription = null,
            modifier = Modifier
                .size(50.dp)
                .clip(RoundedCornerShape(4.dp))
        )
        Spacer(Modifier.width(12.dp))
        Column {
            Text(song.title, maxLines = 1, fontWeight = FontWeight.Bold)
            Text(song.artist, style = MaterialTheme.typography.bodySmall)
        }
    }
}
