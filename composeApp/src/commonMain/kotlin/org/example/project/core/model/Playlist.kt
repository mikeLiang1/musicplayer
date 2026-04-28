package org.example.project.core.model

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class Playlist(
    val id: String,
    val name: String,
    val songs: List<PlaylistSong>,
    val createdAt: Long? = null,
    val updatedAt: Long? = null,
    val thumbnailUrl: String
)
