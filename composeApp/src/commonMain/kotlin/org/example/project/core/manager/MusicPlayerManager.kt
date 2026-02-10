package org.example.project.core.manager

import androidx.media3.common.MediaItem
import kotlinx.coroutines.flow.StateFlow
import org.example.project.core.model.Song

interface MusicPlayerManager {

    val isPlaying: StateFlow<Boolean>
    val currentPosition: StateFlow<Long>
    val duration: StateFlow<Long>
    val currentSong: StateFlow<Song?>

    fun start(song: Song)

    fun pause()
    fun play()

    fun stop()

    fun getCurrentPosition(): Long?

    fun getDuration(): Long?

    fun seekTo(seconds: Long)


    fun release()
}
