package org.example.project.ui.component

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.example.project.ui.theme.Dimens
import org.example.project.ui.theme.appColors


@Composable
fun PlayPauseButton(
    modifier: Modifier = Modifier,
    onPressed: () -> Unit,
    isPlaying: Boolean,
    isBuffering: Boolean = false
) {
    FilledIconButton(
        onClick = onPressed,
        modifier = modifier.size(Dimens.Size.playButton),
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = appColors.iconActive,
            contentColor = appColors.onAccent
        )
    ) {
        if (isBuffering) {
            CircularProgressIndicator(
                modifier = Modifier.size(Dimens.iconM),
                color = appColors.onAccent,
                strokeWidth = Dimens.strokeThick
            )
        } else {
            Icon(
                imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Play",
                modifier = Modifier.size(Dimens.iconL)
            )
        }
    }
}
