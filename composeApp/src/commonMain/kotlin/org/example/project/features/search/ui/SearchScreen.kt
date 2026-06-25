package org.example.project.features.search.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.distinctUntilChanged
import org.example.project.ui.component.SearchBar
import org.example.project.ui.component.SongItem
import org.example.project.ui.theme.AppPreview
import org.example.project.ui.theme.DevicePreviews
import org.example.project.ui.theme.appColors

@Composable
fun SearchScreen(state: SearchUiState, onAction: (SearchAction) -> Unit) {

    BackHandler(enabled = !state.onSearchScreen) { onAction(SearchAction.OnBackPressed) }

    val focusManager = LocalFocusManager.current
    var showPermissionDialog by remember { mutableStateOf(false) }

    if (showPermissionDialog) {
        RequestVoicePermissionEffect {
            showPermissionDialog = false
            onAction(SearchAction.OnVoiceSearch)
        }
    }

    // Drop focus/keyboard whenever a search (text or voice) actually lands on the results
    // screen. Cancelling or erroring out of voice search never flips onSearchScreen, so focus
    // is left alone in that case and the user can keep typing.
    LaunchedEffect(state.onSearchScreen) {
        if (!state.onSearchScreen) {
            focusManager.clearFocus()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        // Search Bar
        SearchBar(
            query = state.searchQuery,
            onQueryChange = { onAction(SearchAction.OnQueryChanged(it)) },
            onVoiceSearch = { showPermissionDialog = true },
            isListening = state.isListening,
            onVoiceSearchCancel = { onAction(SearchAction.OnVoiceSearchCancelled) },
            onSuggestionPressed = { onAction(SearchAction.OnSuggestionClicked(it)) },
            onTextCleared = { onAction(SearchAction.OnTextCleared) },
        )

        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

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
                    if (shouldLoadMore) onAction(SearchAction.SearchMoreSongs)
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
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        // awaitEachGesture is the modern way to listen for touch events
                        awaitEachGesture {
                            // 1. Wait for the initial "Down" (finger hits screen)
                            // 2. We use PointerEventPass.Initial to see the event BEFORE the
                            //    clickable rows inside the list can consume it.
                            awaitFirstDown(pass = PointerEventPass.Initial)

                            // 3. The moment the touch happens, hide the keyboard
                            focusManager.clearFocus()
                        }
                    },
            ) {
                if (onSearchScreen) {
                    // --- SEARCH SUGGESTIONS SCREEN ---
                    items(state.suggestions) { suggestion ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onAction(SearchAction.OnSuggestionClicked(suggestion))
                                }
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(suggestion, color = appColors.textPrimary)
                        }
                    }

                } else {
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

