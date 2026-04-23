package org.example.project.core.database.mapper

import org.example.project.core.database.entity.PlaylistEntity
import org.example.project.core.database.entity.PlaylistSongEntity
import org.example.project.core.database.entity.PlaylistWithSongs
import org.example.project.core.model.Playlist
import org.example.project.core.model.Song

fun Playlist.toEntity() = PlaylistEntity(uniqueId, title, thumbnailUrl)

fun Song.toPlaylistSongEntity(playlistId: String, index: Int): PlaylistSongEntity {
    return PlaylistSongEntity(
        ownerPlaylistId = playlistId,
        songUniqueId = this.uniqueId,
        url = this.url,
        title = this.title,
        artist = this.artist,
        thumbnailUrl = this.thumbnailUrl,
        duration = this.duration,
        orderIndex = index
    )
}

// Convert UI Song -> Room Entity
fun PlaylistSongEntity.toSong(): Song {
    return Song(
        uniqueId = this.songUniqueId,
        url = this.url,
        title = this.title,
        artist = this.artist,
        thumbnailUrl = this.thumbnailUrl,
        duration = this.duration
    )
}


// Convert Room result -> UI Playlist class
fun PlaylistWithSongs.toUIModel(): Playlist {
    return Playlist(
        uniqueId = playlist.playlistId,
        title = playlist.title,
        thumbnailUrl = playlist.thumbnailUrl,
        numSongs = songs.size,
        // Map the database songs to UI songs and sort them by orderIndex
        songs = songs
            .sortedBy { it.orderIndex }
            .map { it.toSong() }
    )
}
