package org.example.project.core.model

import kotlinx.serialization.Serializable


@Serializable
data class PlaylistSong(
    val id: String,        // stable row ID — use as LazyColumn key
    val song: Song,
    val position: Int
)
