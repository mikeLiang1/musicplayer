package org.example.project.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import org.example.project.core.model.Song
import org.example.project.ui.theme.Dimens
import org.example.project.ui.theme.appColors

/**
 * Shared shell for the "screen that shows one collection of songs" pattern —
 * playlist detail and liked songs.
 *
 * Owns the back-arrow top bar, the loading / empty / list switch, and the SongItem
 * rows including the currently-playing highlight. The caller keeps its own item type
 * [T], so PlaylistScreen can pass `PlaylistSong` (retaining the playlistSongId its
 * remove-from-playlist action needs) while LikedSongsScreen passes `Song` directly.
 *
 * [header] renders as the first scrolling item, followed by a divider; pass null for
 * a bare list. When [items] is empty, [emptyMessage] takes the list's place — the
 * header still shows, so a playlist with no songs keeps its cover art.
 */
@Composable
fun <T> SongCollectionScaffold(
    title: String,
    items: List<T>,
    songOf: (T) -> Song,
    itemKey: (T) -> Any,
    onBackPressed: () -> Unit,
    onSongClicked: (item: T, index: Int) -> Unit,
    onSongMenuClicked: (T) -> Unit,
    isLoading: Boolean = false,
    emptyMessage: String = "No songs",
    currentlyPlayingSongId: String? = null,
    isContextActive: Boolean = false,
    isPlaying: Boolean = false,
    topBarActions: @Composable RowScope.() -> Unit = {},
    header: (@Composable () -> Unit)? = null
) {
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .background(color = appColors.backgroundElevated)
                    .fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    IconButton(onClick = onBackPressed) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "back")
                    }
                    Text(
                        text = title,
                        color = appColors.textPrimary,
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                topBarActions()
            }
        }
    ) { innerPadding ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(vertical = Dimens.spaceL),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (header != null) {
                    item { header() }
                    item {
                        HorizontalDivider(
                            color = appColors.divider,
                            modifier = Modifier.padding(vertical = Dimens.spaceM)
                        )
                    }
                }

                if (items.isEmpty()) {
                    item {
                        Text(
                            text = emptyMessage,
                            color = appColors.textMuted,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(Dimens.spaceL)
                        )
                    }
                } else {
                    itemsIndexed(items, key = { _, item -> itemKey(item) }) { index, item ->
                        val song = songOf(item)
                        SongItem(
                            song = song,
                            onClick = { onSongClicked(item, index) },
                            onMenuClicked = { onSongMenuClicked(item) },
                            state = if (currentlyPlayingSongId == song.uniqueId && isContextActive) {
                                SongItemState.Current(isPlaying)
                            } else {
                                SongItemState.Default
                            }
                        )
                    }
                }
            }
        }
    }
}
