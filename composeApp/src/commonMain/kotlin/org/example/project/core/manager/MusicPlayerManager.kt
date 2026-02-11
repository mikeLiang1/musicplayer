package org.example.project.core.manager

import kotlinx.coroutines.flow.StateFlow
import org.example.project.core.model.Song

interface MusicPlayerManager {

    val isPlaying: StateFlow<Boolean>
    val currentPosition: StateFlow<Long>
    val duration: StateFlow<Long>
    val currentSong: StateFlow<Song?>

    suspend fun prepare(song: Song, autoPlay: Boolean = true, startPosition: Long? = null)

    fun pause()
    fun play()

    fun stop()

    fun seekTo(seconds: Long)
}
