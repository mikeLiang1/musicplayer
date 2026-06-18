package org.example.project.core.model

data class SearchResult(
    val songs: List<Song> = emptyList(),
    val albums: List<Album> = emptyList(),
    val artists: List<Artist> = emptyList(),
    val continuationToken: String? = null
)

data class Album(val id: String, val title: String, val artist: String?, val thumbnailUrl: String?)
data class Artist(val id: String, val name: String, val thumbnailUrl: String?)
