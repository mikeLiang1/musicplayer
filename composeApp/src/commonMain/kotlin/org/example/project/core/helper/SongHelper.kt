package org.example.project.core.helper

import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import org.example.project.core.model.Song

fun Song.toMediaItem(): MediaItem {
    val metadata = MediaMetadata.Builder()
        .setTitle(title)
        .setArtist(artist)
        .setSubtitle(artist)
        .setDisplayTitle(title)
        .setArtworkUri(thumbnailUrl?.toUri())
        .setDurationMs(duration)
        .setIsBrowsable(false)
        .setIsPlayable(true)
        .build()
    return MediaItem.Builder()
        .setMediaId(uniqueId)
        .setUri(url)
        .setMediaMetadata(metadata)
        .build()
}

