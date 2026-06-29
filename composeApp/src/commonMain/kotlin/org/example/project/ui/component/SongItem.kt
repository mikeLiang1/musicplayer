package org.example.project.ui.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Reorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import org.example.project.core.helper.formatTime
import org.example.project.core.model.Song
import org.example.project.ui.theme.AppPreview
import org.example.project.ui.theme.DevicePreviews
import org.example.project.ui.theme.Dimens
import org.example.project.ui.theme.appColors

sealed interface SongItemState {
    // 1. Standard song in the list
    data object Default : SongItemState

    // 2. The song currently being played
    data class Current(val isPlaying: Boolean) : SongItemState

    // 3. A song that has already played (usually dimmed)
    data object Previous : SongItemState

    // 4. A song added manually (special background)
    data object Manual : SongItemState
}

@Composable
fun SongItem(
    modifier: Modifier = Modifier,
    song: Song,
    state: SongItemState = SongItemState.Default,
    isEditMode: Boolean = false,
    dragHandleModifier: Modifier = Modifier,
    onMenuClicked: () -> Unit = {},
    onRemoveClicked: () -> Unit = {},
    onClick: () -> Unit
) {
    // 1. Derived Properties based on State
    val backgroundColor = when (state) {
        is SongItemState.Current -> appColors.accentContainer.copy(alpha = 0.3f)
        is SongItemState.Manual -> appColors.rose.copy(alpha = 0.1f)
        else -> Color.Transparent
    }

    val contentAlpha = if (state is SongItemState.Previous) 0.45f else 1f
    val titleColor = if (state is SongItemState.Current) appColors.accentPrimary else appColors.textPrimary

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .clickable { onClick() }
            .alpha(contentAlpha)
            .padding(vertical = Dimens.spaceS)
            .padding(start = Dimens.spaceM),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.spaceM)
    ) {
        // 2. Conditional Drag Handle
        if (isEditMode) {
            Icon(
                imageVector = Icons.Default.Reorder,
                contentDescription = null,
                tint = appColors.iconSecondary,
                modifier = Modifier
                    .then(dragHandleModifier)
            )
        }

        // 3. Cover Image + Equalizer Logic
        Box(modifier = Modifier.size(Dimens.Size.coverThumb)) {
            CoverImage(
                data = song.thumbnailUrl,
                modifier = Modifier
                    .fillMaxSize(),
                shape = RoundedCornerShape(Dimens.radiusM)
            )

            if (state is SongItemState.Current) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(Dimens.radiusM))
                        .background(Color.Black.copy(alpha = 0.45f)),
                    contentAlignment = Alignment.Center
                ) {
                    EqualizerBars(state.isPlaying)
                }
            }
        }

        // 4. Current Song Accent Bar
        if (state is SongItemState.Current) {
            Box(
                modifier = Modifier
                    .width(Dimens.spaceXs)
                    .height(Dimens.Size.coverThumb)
                    .clip(CircleShape)
                    .background(appColors.accentPrimary)
            )
        }

        // 5. Texts
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                maxLines = 1,
                fontWeight = FontWeight.SemiBold,
                color = titleColor,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${song.artist} • ${formatTime(song.duration)}",
                style = MaterialTheme.typography.bodySmall,
                color = appColors.textMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // 6. Action Button
        IconButton(onClick = if (isEditMode) onRemoveClicked else onMenuClicked) {
            Icon(
                imageVector = if (isEditMode) Icons.Filled.Close else Icons.Filled.MoreVert,
                contentDescription = null,
                tint = appColors.iconSecondary
            )
        }
    }
}

@Composable
private fun EqualizerBars(isPlaying: Boolean) {
    val bars = listOf(0.3f, 0.7f, 1f, 0.5f)
    val lifecycle = LocalLifecycleOwner.current

    Row(
        horizontalArrangement = Arrangement.spacedBy(Dimens.spaceXxs),
        verticalAlignment = Alignment.Bottom,
        modifier = Modifier.height(Dimens.Size.equalizerBarHeight)
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
                    .width(Dimens.spaceXs)
                    .height(Dimens.Size.equalizerBarHeight)
                    .clip(RoundedCornerShape(Dimens.radiusS))
                    .graphicsLayer {
                        scaleY = scale.value
                        transformOrigin = TransformOrigin(0.5f, 1f)
                    }
                    .background(appColors.accentPrimary)
            )
        }
    }
}


@DevicePreviews
@Composable
private fun SongItemPreview() {
    val mockSong = Song(
        url = "url",
        title = "Song Title",
        artist = "Artist Name",
        thumbnailUrl = "https://example.com/image.jpg",
        duration = 180000L // 3 minutes
    )

    AppPreview {
        Column(
            verticalArrangement = Arrangement.spacedBy(Dimens.spaceM),
            modifier = Modifier.padding(Dimens.spaceL)
        ) {
            // 1. CURRENTLY PLAYING
            Text("Current State", style = MaterialTheme.typography.labelSmall)
            SongItem(
                song = mockSong.copy(title = "Currently Playing Song"),
                state = SongItemState.Current(isPlaying = true),
                onClick = {}
            )

            // 2. MANUAL QUEUE + REMOVE BUTTON
            Text("Manual State", style = MaterialTheme.typography.labelSmall)
            SongItem(
                song = mockSong.copy(title = "Manual Queue Song"),
                state = SongItemState.Manual,
                isEditMode = true,
                onRemoveClicked = {},
                onClick = {}
            )

            // 3. EDITABLE QUEUE
            Text("Editable State", style = MaterialTheme.typography.labelSmall)
            SongItem(
                song = mockSong.copy(title = "Normal Editable Queue Song"),
                state = SongItemState.Default,
                isEditMode = true,
                onRemoveClicked = {},
                onClick = {}
            )

            // 4. NORMAL / DEFAULT
            Text("Default State", style = MaterialTheme.typography.labelSmall)
            SongItem(
                song = mockSong.copy(title = "Normal Queue Song"),
                state = SongItemState.Default,
                onMenuClicked = {},
                onClick = {}
            )

            // 5. HISTORY / PREVIOUS
            Text("Previous State", style = MaterialTheme.typography.labelSmall)
            SongItem(
                song = mockSong.copy(title = "History Song"),
                state = SongItemState.Previous,
                onMenuClicked = {},
                onClick = {}
            )
        }
    }
}
