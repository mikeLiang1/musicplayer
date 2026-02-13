package org.example.project.core.helper

import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import org.example.project.core.model.Song

fun Song.toMediaItem(): MediaItem {
    val metadata = MediaMetadata.Builder()
        .setTitle(title)
        .setArtist(artist)
        .setArtworkUri(thumbnailUrl?.toUri())
        .setDurationMs(duration)
        .build()
    return MediaItem.Builder()
        .setMediaId(url)
        .setUri(url)
        .setMediaMetadata(metadata)
        .build()
}

fun MediaItem.toSong(): Song {

    return Song(
        url = mediaId, // Retrieved from mediaId
        title = mediaMetadata.title?.toString() ?: "Unknown",
        artist = mediaMetadata.artist?.toString() ?: "Unknown",
        thumbnailUrl = mediaMetadata.artworkUri?.toString(),
        duration = mediaMetadata.durationMs ?: 0L
    )
}
