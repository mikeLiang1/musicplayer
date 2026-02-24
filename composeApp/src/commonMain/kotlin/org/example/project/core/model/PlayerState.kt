package org.example.project.core.model

import androidx.compose.runtime.Stable

@Stable
data class PlayerState(
    val currentSong: Song? = null,
    val isPlaying: Boolean = false,
    val currentIndex: Int = 0,
    val queue: List<Song> = listOf(),
    val originalQueue: List<Song> = listOf(),
    val isShuffled: Boolean = false
)
