package org.example.project.core.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LastPage
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.vector.ImageVector

@Stable
data class PlayerState(
    val isPlaying: Boolean = false,
    val durationMs: Long = 0L
)
