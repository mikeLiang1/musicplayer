package org.example.project.core.model

import androidx.compose.runtime.Stable

@Stable
data class PlayerState(
    val isPlaying: Boolean = false,
    val durationMs: Long = 0L,
    val isBuffering: Boolean = false
)
