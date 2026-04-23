package org.example.project.features.playlist.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.example.project.core.database.MusicDatabase
import org.example.project.core.database.entity.PlaylistEntity
import org.example.project.core.database.entity.PlaylistSongEntity
import org.example.project.core.database.entity.PlaylistWithSongs
import org.example.project.core.model.Playlist
import org.example.project.core.model.Song

class PlaylistRepository(database: MusicDatabase) {
    private val dao = database.playlistDao()

    // 1. Observe playlists (Updates UI in real-time)
    val allPlaylists: Flow<List<Playlist>> = dao.getAllPlaylistsWithSongs()
        .map { list -> list.map { it.toUIModel() } }

    // 2. Add a new playlist
    suspend fun savePlaylist(playlist: Playlist) {
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
}


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
