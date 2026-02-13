package org.example.project.core.model

import kotlinx.serialization.Serializable

@Serializable
data class Song(
    val url: String,
    val title: String,
    val artist: String,
    val thumbnailUrl: String?,
    val duration: Long
)
