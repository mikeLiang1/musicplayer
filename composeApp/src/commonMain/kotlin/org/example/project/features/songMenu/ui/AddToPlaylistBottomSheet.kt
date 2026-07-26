package org.example.project.features.songMenu.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import org.example.project.core.model.Playlist
import org.example.project.ui.component.PlaylistItem
import org.example.project.ui.theme.Dimens
import org.example.project.ui.theme.appColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddToPlaylistBottomSheet(
    isBottomSheetVisible: Boolean,
    onCloseBottomSheet: () -> Unit,
    playlists: List<Playlist>,
    isSongLiked: Boolean,
    likedSongCount: Int,
    checkedPlaylistIds: Set<String>,
    onLikedSongsClicked: () -> Unit,
    onPlaylistClicked: (String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(isBottomSheetVisible) {
        if (isBottomSheetVisible) {
            sheetState.show()
        } else {
            sheetState.hide()
        }
    }
    if (isBottomSheetVisible) {
        // Every row is an add/remove toggle and the sheet stays open, so the song can be moved in
        // and out of several playlists in one pass with each row updating as you go.
        ModalBottomSheet(
            onDismissRequest = onCloseBottomSheet,
            sheetState = sheetState,
            containerColor = appColors.backgroundElevated
        ) {
            LazyColumn(
                contentPadding = PaddingValues(bottom = Dimens.spaceL),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Liked Songs is always offered, even before any playlist exists.
                item(key = "liked_songs") {
                    LikedSongsRow(
                        isSongLiked = isSongLiked,
                        songCount = likedSongCount,
                        onClick = onLikedSongsClicked
                    )
                    HorizontalDivider(
                        color = appColors.dividerSubtle,
                        thickness = Dimens.strokeThin,
                        modifier = Modifier.padding(
                            horizontal = Dimens.spaceM,
                            vertical = Dimens.spaceS
                        )
                    )
                }

                if (playlists.isEmpty()) {
                    item(key = "empty") {
                        Text(
                            text = "No playlists yet",
                            style = MaterialTheme.typography.bodyMedium,
                            color = appColors.textMuted,
                            modifier = Modifier.padding(Dimens.spaceL)
                        )
                    }
                } else {
                    items(playlists, key = { it.id }) { playlist ->
                        PlaylistItem(
                            playlist = playlist,
                            onClick = { onPlaylistClicked(playlist.id) },
                            trailing = {
                                Checkbox(
                                    checked = playlist.id in checkedPlaylistIds,
                                    // The whole row is the click target; the box shows state only.
                                    onCheckedChange = null,
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = appColors.accentPrimary,
                                        uncheckedColor = appColors.iconSecondary,
                                        checkmarkColor = appColors.onAccent
                                    ),
                                    modifier = Modifier.padding(end = Dimens.spaceM)
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LikedSongsRow(
    isSongLiked: Boolean,
    songCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(Dimens.spaceS),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(Dimens.Size.coverThumb)
                .background(
                    color = appColors.accentDark,
                    shape = RoundedCornerShape(Dimens.radiusS)
                )
        ) {
            Icon(
                imageVector = Icons.Rounded.Favorite,
                contentDescription = null,
                tint = appColors.onAccent,
                modifier = Modifier.size(Dimens.iconM)
            )
        }

        Spacer(Modifier.width(Dimens.spaceM))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Liked Songs",
                fontWeight = FontWeight.SemiBold,
                color = appColors.textPrimary
            )
            Spacer(modifier = Modifier.height(Dimens.spaceXs))
            Text(
                text = if (isSongLiked) "Added • $songCount songs" else "$songCount songs",
                style = MaterialTheme.typography.bodySmall,
                color = appColors.textMuted
            )
        }

        Icon(
            imageVector = if (isSongLiked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
            contentDescription = if (isSongLiked) "Remove from liked songs" else "Add to liked songs",
            tint = if (isSongLiked) appColors.rose else appColors.iconSecondary,
            modifier = Modifier
                .padding(end = Dimens.spaceM)
                .size(Dimens.iconM)
        )
    }
}
