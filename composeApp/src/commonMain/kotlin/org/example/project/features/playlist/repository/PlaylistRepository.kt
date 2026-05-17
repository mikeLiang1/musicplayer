package org.example.project.features.playlist.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.example.project.core.database.MusicDatabase
import org.example.project.core.database.entity.PlaylistEntity
import org.example.project.core.database.mapper.toDomain
import org.example.project.core.database.mapper.toSong
import org.example.project.core.database.mapper.toSongEntity
import org.example.project.core.model.Playlist
import org.example.project.core.model.Song
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class PlaylistRepository(database: MusicDatabase, private val clock: Clock = Clock.System) {
    private val dao = database.playlistDao()

    fun getPlaylists(): Flow<List<Playlist>> =
        dao.getAllPlaylistsWithSongs().map { list -> list.map { it.toDomain() } }

    fun getPlaylist(id: String): Flow<Playlist?> =
        dao.getPlaylistWithSongs(id).map { it?.toDomain() }

    @OptIn(ExperimentalUuidApi::class)
    suspend fun createPlaylist(name: String): Playlist {
        val now = clock.now().toEpochMilliseconds()
        val playlist = PlaylistEntity(
            id = Uuid.random().toString(),
            name = name,
            createdAt = now,
            updatedAt = now,
            thumbnailUrl = ""
        )
        dao.insertPlaylist(playlist)
        return Playlist(
            id = playlist.id,
            name = playlist.name,
            songs = emptyList(),
            createdAt = playlist.createdAt,
            updatedAt = playlist.updatedAt,
            thumbnailUrl = playlist.thumbnailUrl
        )
    }

    suspend fun renamePlaylist(id: String, name: String) {
        dao.renamePlaylist(id, name, clock.now().toEpochMilliseconds())
    }

    suspend fun deletePlaylist(id: String) {
        dao.deletePlaylist(id)
    }

//    suspend fun getSong(id: String): Song? {
//        return dao.getSong(id)?.toSong()
//    }

    suspend fun addSong(playlistId: String, song: Song) {
        val now = clock.now().toEpochMilliseconds()
        dao.addSongToPlaylist(
            playlistId = playlistId,
            song = song.toSongEntity(firstAddedAt = now),
            timestamp = now
        )
    }

    suspend fun removePlaylistSong(playlistSongId: String) {
        dao.deletePlaylistSong(playlistSongId)
    }

    suspend fun reorderSongs(playlistId: String, playlistSongIds: List<String>) {
        val current = dao.getPlaylistWithSongs(playlistId).first() ?: return
        val byId = current.songs.associateBy { it.playlistSong.id }
        val reordered = playlistSongIds.mapIndexedNotNull { index, id ->
            byId[id]?.playlistSong?.copy(position = index)
        }
        dao.replaceAllPlaylistSongs(
            playlistId = playlistId,
            playlistSongs = reordered,
            timestamp = clock.now().toEpochMilliseconds()
        )
    }
}


