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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.distinctUntilChanged
import org.example.project.features.dashboard.navigation.bottomBarDp
import org.example.project.ui.component.MusicSearchBar
import org.example.project.ui.component.SongItem
import org.example.project.ui.theme.AppPreview
import org.example.project.ui.theme.DevicePreviews
import org.example.project.ui.theme.appColors

@Composable
fun SearchScreen(state: SearchUiState, onAction: (SearchAction) -> Unit) {

//    LaunchedEffect(Unit) {
//        searchViewModel.effect.collect { effect ->
//            when (effect) {
//                is SearchEffect.NavigateToResult -> {
//                    navigateToResult()
//                }
//            }
//        }
//    }

    BackHandler(enabled = !state.onSearchScreen) { onAction(SearchAction.OnBackPressed) }

    val focusManager = LocalFocusManager.current

    val interactionSource = remember { MutableInteractionSource() }

// This block listens for a "Release" event, which is effectively a click
    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { interaction ->
            if (interaction is PressInteraction.Release) {
                onAction(SearchAction.OnBackPressed)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(top = 16.dp)
    ) {
        // Search Bar
        MusicSearchBar(
            query = state.searchQuery,
            onQueryChange = { onAction(SearchAction.OnQueryChanged(it)) },
            onVoiceSearch = {},
            onSuggestionPressed = { onAction(SearchAction.OnSuggestionClicked(it)) }
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
                    if (shouldLoadMore && !state.isLoadingMore && !state.onSearchScreen && !state.isLoading) {
                        onAction(SearchAction.SearchMoreSongs)
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
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 16.dp, bottom = bottomBarDp + 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (onSearchScreen) {
                    // --- SEARCH SUGGESTIONS SCREEN ---
                    items(state.suggestions) { suggestion ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onAction(SearchAction.OnSuggestionClicked(suggestion))
                                    focusManager.clearFocus()
                                }
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(suggestion, color = appColors.textPrimary)
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
                        SongItem(
                            song = song,
                            onMenuClicked = {

                            }, onClick = {
                                onAction(SearchAction.OnSongClicked(song))
                            }
                        )
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

@DevicePreviews
@Composable
private fun SearchBarPreview() {
    AppPreview {
        SearchScreen(state = SearchUiState()) { }

    }
}

