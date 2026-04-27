package org.example.project.features.playlist.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.example.project.core.database.MusicDatabase
import org.example.project.core.database.mapper.toEntity
import org.example.project.core.database.mapper.toPlaylistSongEntity
import org.example.project.core.database.mapper.toUIModel
import org.example.project.core.model.Playlist
import org.example.project.core.model.Song

class PlaylistRepository(database: MusicDatabase) {
    private val dao = database.playlistDao()

    // 1. Observe playlists (Updates UI in real-time)
    val allPlaylists: Flow<List<Playlist>> = dao.getAllPlaylists()
        .map { list -> list.map { it.toUIModel() } }

    // 2. Add a new playlist
    suspend fun saveEntirePlaylist(playlist: Playlist) {
        val playlistEntity = playlist.toEntity()
        val songEntities = playlist.songs.mapIndexed { index, song ->
            song.toPlaylistSongEntity(playlistId = playlist.uniqueId, index = index)
        }
        dao.saveFullPlaylist(playlistEntity, songEntities)
    }

    suspend fun addSongToPlaylist(playlistId: String, song: Song) {
        // 1. Find the current last position (if empty, start at -1)
        val currentMaxIndex = dao.getMaxOrderIndex(playlistId) ?: -1
        val nextIndex = currentMaxIndex + 1

        // 2. Convert your UI Song model to the Database Entity
        val entity = song.toPlaylistSongEntity(playlistId = playlistId, index = nextIndex)

        // 3. Save it
        dao.insertPlaylistSong(entity)
    }

    fun getSongsFromPlaylist(playListId: String): Flow<Playlist?> {
        return dao.getSongsFromPlaylist(playListId).map { it?.toUIModel() }
    }

    suspend fun deleteSongFromPlaylist(playlistId: String, songId: String) {
        dao.deleteSongFromPlaylist(playlistId, songId)
    }

}


