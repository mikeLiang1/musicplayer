package org.example.project.core.model

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class Song(
    val uniqueId: String = UUID.randomUUID().toString(),
    val url: String,
    val title: String,
    val artist: String,
    val thumbnailUrl: String?,
    val duration: Long
)
