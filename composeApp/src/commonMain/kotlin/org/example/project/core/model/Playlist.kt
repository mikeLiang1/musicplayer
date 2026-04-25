package org.example.project.core.model

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class Playlist(
    val uniqueId: String = UUID.randomUUID().toString(),
    val title: String,
    val thumbnailUrl: String? = null,
    val numSongs: Int,
    val songs: List<Song> = listOf()
)
