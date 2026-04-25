package org.example.project.ui.component

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.example.project.ui.theme.appColors


@Composable
fun PlayPauseButton(onPressed: () -> Unit, isPlaying: Boolean, modifier: Modifier = Modifier) {
    FilledIconButton(
        onClick = onPressed,
        modifier = modifier.size(64.dp),
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = appColors.iconActive,
            contentColor = Color.White
        )
    ) {
        Icon(
            imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
            contentDescription = if (isPlaying) "Pause" else "Play",
            modifier = Modifier.size(32.dp)
        )
    }
}
