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
