package org.example.project.core.manager

import kotlinx.coroutines.flow.StateFlow
import org.example.project.core.model.Song
import org.example.project.core.model.PlayerState

interface MusicPlayerManager {

    val playerState: StateFlow<PlayerState>

    val currentPosition: StateFlow<Long>

    fun initialise()

    suspend fun prepare(song: Song, autoPlay: Boolean = true, startPosition: Long? = null)
    fun pause()
    fun play()
    suspend fun setQueue(songs: List<Song>)
    fun stop()
    fun seekTo(seconds: Long)
    fun skipToNext()
    fun skipToPrevious()
    fun hasNext(): Boolean
    fun hasPrevious(): Boolean
}
