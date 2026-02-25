package org.example.project.core.model

import androidx.compose.runtime.Stable

@Stable
data class PlayerState(
    val currentSong: Song? = null,
    val currentIndex: Int = 0,
    val isPlaying: Boolean = false,
    val queue: List<Song> = listOf(),
    val originalQueue: List<Song> = listOf(),
    val isShuffled: Boolean = false,
)
