package org.example.project.features.musicPlayer.ui

import androidx.annotation.OptIn
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import org.example.project.core.model.Song
import org.example.project.ui.component.CoverImage
import org.example.project.ui.theme.appColors

@OptIn(UnstableApi::class)
@Composable
fun MusicPlayerBar(viewModel: MusicPlayerViewModel, modifier: Modifier = Modifier) {
    val state by viewModel.playerState.collectAsStateWithLifecycle()
    val displayQueue by viewModel.displayQueue.collectAsStateWithLifecycle()

    val currentPosition by viewModel.currentPosition.collectAsStateWithLifecycle()

    displayQueue.current?.let { song ->
        Surface(
            color = appColors.backgroundElevated,
            modifier = modifier
                .height(65.dp)
                .clickable(indication = null, interactionSource = null) { viewModel.setFullScreen(true) }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Song info with thumbnail — swipe the carousel to change songs
                    SongPager(
                        queue = displayQueue,
                        onSongSelected = viewModel::changePlayingToSong,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) { song ->
                        SongInfoRow(
                            song = song,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    // Playback controls
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = viewModel::onPreviousClicked
                        ) {
                            Icon(
                                imageVector = Icons.Filled.SkipPrevious,
                                contentDescription = "Previous",
                                tint = appColors.iconPrimary
                            )
                        }
                        // Play/Pause button
                        IconButton(
                            onClick = viewModel::onPlayPauseClicked
                        ) {
                            if (state.isBuffering) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = appColors.iconPrimary,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                    contentDescription = if (state.isPlaying) "Pause" else "Play",
                                    tint = appColors.iconPrimary
                                )
                            }
                        }

                        // Next button
                        IconButton(
                            onClick = viewModel::onNextClicked
                        ) {
                            Icon(
                                imageVector = Icons.Filled.SkipNext,
                                contentDescription = "Next",
                                tint = appColors.iconPrimary
                            )
                        }
                    }
                }
                LinearProgressIndicator(
                    progress = {
                        if (song.duration > 0) currentPosition.toFloat() / song.duration.toFloat()
                        else 0f
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter),
                    color = appColors.accentDark,
                    trackColor = appColors.backgroundSurface,
                )
            }
        }
    }
}

@Composable
private fun SongInfoRow(song: Song, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CoverImage(song.thumbnailUrl, size = 48.dp, shape = RoundedCornerShape(4.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = appColors.textPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = song.artist,
                style = MaterialTheme.typography.bodySmall,
                color = appColors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
