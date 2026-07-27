package org.example.project.ui.component

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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import org.example.project.core.model.Song
import org.example.project.ui.theme.Dimens
import org.example.project.ui.theme.appColors

/**
 * Shared shell for the "screen that shows one collection of songs" pattern —
 * playlist detail and liked songs.
 *
 * Owns the back-arrow top bar, the loading / empty / list switch, and the SongItem
 * rows including the currently-playing highlight.
 *
 * Rows are keyed on `Song.uniqueId`, which both callers make unique per row: playlist
 * songs carry their playlist_songs row ID, liked songs their URL. That same ID is what
 * PlaylistScreen hands to remove-from-playlist, so no wrapper type is needed here.
 *
 * [header] renders as the first scrolling item, followed by a divider; pass null for
 * a bare list. When [songs] is empty, [emptyMessage] takes the list's place — the
 * header still shows, so a playlist with no songs keeps its cover art.
 */
@Composable
fun SongCollectionScaffold(
    title: String,
    songs: List<Song>,
    onBackPressed: () -> Unit,
    onSongClicked: (song: Song, index: Int) -> Unit,
    onSongMenuClicked: (Song) -> Unit,
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
            // No background of its own — an elevated fill here reads as a stray colour band
            // with a hard edge partway down the screen. The bar sits on the page background.
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = Dimens.spaceXs)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    IconButton(onClick = onBackPressed) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "back",
                            tint = appColors.iconPrimary
                        )
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
                contentPadding = PaddingValues(bottom = Dimens.spaceL)
            ) {
                if (header != null) {
                    item { header() }
                    item {
                        HorizontalDivider(
                            color = appColors.dividerSubtle,
                            modifier = Modifier.padding(bottom = Dimens.spaceS)
                        )
                    }
                }

                if (songs.isEmpty()) {
                    item {
                        // fillMaxWidth + centred text rather than a centred LazyColumn: aligning
                        // the whole column would drag every full-width row's content around too.
                        Text(
                            text = emptyMessage,
                            color = appColors.textMuted,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(Dimens.spaceL)
                        )
                    }
                } else {
                    itemsIndexed(songs, key = { _, song -> song.uniqueId }) { index, song ->
                        SongItem(
                            song = song,
                            onClick = { onSongClicked(song, index) },
                            onMenuClicked = { onSongMenuClicked(song) },
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
