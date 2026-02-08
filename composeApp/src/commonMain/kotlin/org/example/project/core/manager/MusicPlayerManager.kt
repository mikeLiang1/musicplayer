package org.example.project.core.manager

import androidx.media3.common.MediaItem
import org.example.project.core.model.Song

interface MusicPlayerManager {

    fun start(song: Song)

    fun pause()

    fun stop()

    fun getCurrentPosition(): Long?

    fun getDuration(): Long?

    fun seekTo(seconds: Long)

    fun isPlaying(): Boolean

    fun release()
}
