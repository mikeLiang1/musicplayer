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
    val duration: Long,
    val album: String? = null
)

data class SongPage(
    val songs: List<Song>,
    val continuationToken: String? // null = no more pages
)

private val mockSong =  Song(
    url = "item.url",
    title = "Currently Playing Song",
    artist = "Artist",
    thumbnailUrl = "item.thumbnails.firstOrNull()?.url",
    duration = 3000L
)
val mockSongList = listOf(mockSong, mockSong, mockSong, mockSong)
