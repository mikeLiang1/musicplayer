package org.example.project.features.library.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.example.project.core.model.Playlist
import org.example.project.features.library.model.LibraryItem
import org.example.project.features.library.model.stableKey
import org.example.project.ui.component.PlaylistItem
import org.example.project.ui.component.SongItem
import org.example.project.ui.theme.AppPreview
import org.example.project.ui.theme.DevicePreviews
import org.example.project.ui.theme.appColors

@Composable
fun LibraryScreen(state: LibraryUiState, onAction: (LibraryAction) -> Unit) {
    Scaffold(
        topBar = {
            Column(modifier = Modifier.statusBarsPadding()) {
                HeaderSection(onAction = onAction, state = state)
                HorizontalDivider(color = appColors.divider)
            }
        }
    ) { padding ->
        LibraryColumn(modifier = Modifier.padding(padding), state = state, onAction = onAction)
    }
}


@Composable
private fun HeaderSection(onAction: (LibraryAction) -> Unit, state: LibraryUiState) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Library", color = appColors.textPrimary, style = MaterialTheme.typography.headlineLarge)

            Icon(Icons.Default.Add, tint = appColors.iconPrimary, contentDescription = "add playlist")
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            LibraryItemFilter.entries.forEach { it ->
                LibraryFilterPill(
                    libraryItemFilter = it,
                    onClick = { onAction(LibraryAction.OnFilterSelected(it)) },
                    isSelected = it == state.selectedFilter
                )
            }
        }
    }

}

@Composable
private fun LibraryFilterPill(
    modifier: Modifier = Modifier,
    libraryItemFilter: LibraryItemFilter,
    onClick: () -> Unit,
    isSelected: Boolean
) {
    Box(
        modifier = modifier
            .clip(CircleShape) // 1. Clip first to ensure the ripple and background are pill-shaped
            .background(if (isSelected) appColors.accentContainer else Color.Transparent)
            .border(
                width = 1.dp,
                color = if (isSelected) appColors.accentDark else appColors.divider,
                shape = CircleShape // 2. Must match the clip shape
            )
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 16.dp) // 3. Padding inside the clickable area
    ) {
        Text(
            text = libraryItemFilter.name,
            color = if (isSelected) appColors.accentPrimary else appColors.textSecondary,
            style = MaterialTheme.typography.labelLarge
        )
    }
}

@Composable
private fun LibraryColumn(modifier: Modifier = Modifier, state: LibraryUiState, onAction: (LibraryAction) -> Unit) {
    LazyColumn(modifier = modifier) {
        item {
            LikedSongBanner(songCount = state.likedSongCount, onClick = {})
        }

        items(state.libraryItems, key = { it.stableKey() }) { item ->
            when (item) {
                is LibraryItem.PlaylistItem -> {
                    PlaylistItem(
                        onClick = { onAction(LibraryAction.OnPlayListSelected(item.playlist.uniqueId)) },
                        playlist = Playlist(title = "sad", thumbnailUrl = null, numSongs = 30)
                    )
                }

                is LibraryItem.SongItem -> {
                    SongItem(onClick = {}, song = item.song)
                }
            }
        }
    }
}

@Composable
private fun LikedSongBanner(
    songCount: Int,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp)
            .height(100.dp), // Slightly taller to feel like a "Hero" item
        shape = RoundedCornerShape(16.dp),
        color = appColors.accentContainer
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Icon Container
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(
                        color = appColors.onAccentContainer,
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Favorite,
                    contentDescription = null,
                    tint = appColors.iconPrimary,
                    modifier = Modifier.size(32.dp)
                )
            }

            // 2. Text Info
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp)
            ) {
                Text(
                    text = "Liked Songs",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.sp
                    ),
                    color = appColors.textPrimary
                )
                Text(
                    text = "$songCount songs",
                    style = MaterialTheme.typography.bodyMedium,
                    color = appColors.textSecondary
                )
            }

        }
    }
}

@DevicePreviews
@Composable
private fun LibraryPreview() {
    AppPreview {
        LibraryScreen(state = LibraryUiState(), onAction = {})
    }
}
