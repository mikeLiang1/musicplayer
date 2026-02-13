package org.example.project.core.model

import androidx.compose.runtime.Stable

@Stable
data class PlayerState(
    val currentSong: Song? = null,
    val isPlaying: Boolean = false,
    val duration: Long = 0L,
    val queue: List<Song> = emptyList(),
    val currentIndex: Int = 0
)
