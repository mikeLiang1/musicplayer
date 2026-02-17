package org.example.project.core.model

import androidx.compose.runtime.Stable

@Stable
data class PlayerState(
    val currentSong: Song? = null,
    val isPlaying: Boolean = false,
    val queue: List<Song> = emptyList(),
    val currentIndex: Int = 0
) {
    val upcomingQueue: List<Song>
        get() = queue.drop(currentIndex)
    val previousInQueue: List<Song>
        get() = queue.take(currentIndex)
}
