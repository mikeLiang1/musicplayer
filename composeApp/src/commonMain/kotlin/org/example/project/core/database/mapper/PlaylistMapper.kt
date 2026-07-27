package org.example.project.core.database.mapper

import org.example.project.core.database.entity.PlaylistSongWithSong
import org.example.project.core.database.entity.PlaylistWithSongs
import org.example.project.core.database.entity.SongEntity
import org.example.project.core.model.Playlist
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

/**
 * The playlist_songs row ID becomes the song's `uniqueId`, so two copies of the same song
 * in one playlist stay distinguishable and removal can be keyed straight off the Song.
 * [PlaylistSongWithSong.playlistSong]'s position is applied by the caller's sort.
 */
internal fun PlaylistSongWithSong.toDomain() = song.toSong(idOverride = playlistSong.id)

internal fun SongEntity.toSong(idOverride: String, isLiked: Boolean = false) = Song(
    uniqueId = idOverride, // Use the DB ID instead of random()
    url = url,
    title = title,
    artist = artist,
    thumbnailUrl = thumbnailUrl,
    duration = duration,
    isLiked = isLiked
)

internal fun PlaylistWithSongs.toDomain() = Playlist(
    id = playlist.id,
    name = playlist.name,
    createdAt = playlist.createdAt,
    updatedAt = playlist.updatedAt,
    songs = songs.sortedBy { it.playlistSong.position }.map { it.toDomain() },
    thumbnailUrl = playlist.thumbnailUrl
)
