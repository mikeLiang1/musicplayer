package org.example.project.core.model

data class PlaybackState(
    val currentSongId: String?,
    val positionMs: Long,
    val queue: List<Song> = emptyList()
)
