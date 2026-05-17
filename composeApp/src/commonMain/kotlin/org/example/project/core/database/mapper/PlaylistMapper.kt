package org.example.project.core.database.mapper

import org.example.project.core.database.entity.PlaylistSongWithSong
import org.example.project.core.database.entity.PlaylistWithSongs
import org.example.project.core.database.entity.SongEntity
import org.example.project.core.model.Playlist
import org.example.project.core.model.PlaylistSong
import org.example.project.core.model.Song
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid


internal fun Song.toSongEntity(firstAddedAt: Long) = SongEntity(
    url = url,
    title = title,
    artist = artist,
    thumbnailUrl = thumbnailUrl,
    duration = duration,
    firstAddedAt = firstAddedAt
)

internal fun PlaylistSongWithSong.toDomain() = PlaylistSong(
    id = playlistSong.id,
    song = song.toSong(idOverride = playlistSong.id), // Pass the persistent ID
    position = playlistSong.position
)

internal fun SongEntity.toSong(idOverride: String) = Song(
    uniqueId = idOverride, // Use the DB ID instead of random()
    url = url,
    title = title,
    artist = artist,
    thumbnailUrl = thumbnailUrl,
    duration = duration
)

internal fun PlaylistWithSongs.toDomain() = Playlist(
    id = playlist.id,
    name = playlist.name,
    createdAt = playlist.createdAt,
    updatedAt = playlist.updatedAt,
    songs = songs.sortedBy { it.playlistSong.position }.map { it.toDomain() },
    thumbnailUrl = playlist.thumbnailUrl
)
