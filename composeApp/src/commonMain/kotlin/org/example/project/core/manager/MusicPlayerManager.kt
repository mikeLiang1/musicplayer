package org.example.project.core.manager

import androidx.media3.common.MediaItem

interface MusicPlayerManager {

    fun start()

    fun pause()

    fun stop()

    fun getCurrentPosition(): Long?

    fun getDuration(): Long?

    fun seekTo(seconds: Long)

    fun isPlaying(): Boolean

    fun release()
}
