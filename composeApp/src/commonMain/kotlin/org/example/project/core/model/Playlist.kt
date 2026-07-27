package org.example.project.core.model

import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * [songs] are ordered by their playlist position, and each song's `uniqueId` is the
 * playlist_songs row ID — so the same song added twice yields two distinct entries,
 * and that ID is what removing a song from this playlist is keyed on.
 */
@Serializable
data class Playlist(
    val id: String,
    val name: String,
    val songs: List<Song>,
    val createdAt: Long? = null,
    val updatedAt: Long? = null,
    val thumbnailUrl: String
)
