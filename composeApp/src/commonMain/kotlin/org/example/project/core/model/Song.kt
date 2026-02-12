package org.example.project.core.model

import androidx.media3.common.MediaItem
import kotlinx.serialization.Serializable
import org.example.project.core.helper.secondsToDuration

@Serializable
data class Song(
    val url: String,
    val title: String,
    val artist: String,
    val thumbnailUrl: String?,
    val duration: String?
)


fun MediaItem.toSong(): Song {

    return Song(
        url = mediaId, // Retrieved from mediaId
        title = mediaMetadata.title?.toString() ?: "Unknown",
        artist = mediaMetadata.artist?.toString() ?: "Unknown",
        thumbnailUrl = mediaMetadata.artworkUri?.toString(),
        duration = mediaMetadata.durationMs?.secondsToDuration()
    )
}
