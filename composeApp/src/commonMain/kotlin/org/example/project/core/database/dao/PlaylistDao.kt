package org.example.project.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import org.example.project.core.database.entity.PlaylistEntity
import org.example.project.core.database.entity.PlaylistSongEntity
import org.example.project.core.database.entity.PlaylistWithSongs

@Dao
interface PlaylistDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: PlaylistEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSongs(songs: List<PlaylistSongEntity>)

    @Query("DELETE FROM playlist_songs WHERE ownerPlaylistId = :playlistId")
    suspend fun deleteSongsFromPlaylist(playlistId: String)

    @Query("DELETE FROM playlists WHERE playlistId = :playlistId")
    suspend fun deleteEntirePlaylist(playlistId: String)

    @Transaction
    @Query("SELECT * FROM playlists WHERE playlistId = :id")
    fun getSongsFromPlaylist(id: String): Flow<PlaylistWithSongs?>

    // Get all playlists (Flow updates automatically when data changes)
    @Transaction
    @Query("SELECT * FROM playlists")
    fun getAllPlaylists(): Flow<List<PlaylistWithSongs>>


    // High-level function to save/update a whole playlist safely
    @Transaction
    suspend fun saveFullPlaylist(playlist: PlaylistEntity, songs: List<PlaylistSongEntity>) {
        insertPlaylist(playlist)
        // We delete old versions of songs for this playlist to avoid duplicates/order issues
        deleteSongsFromPlaylist(playlist.playlistId)
        insertSongs(songs)
    }

    // Add song to playlist
    @Query("SELECT MAX(orderIndex) FROM playlist_songs WHERE ownerPlaylistId = :playlistId")
    suspend fun getMaxOrderIndex(playlistId: String): Int?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylistSong(songEntity: PlaylistSongEntity)

    @Query("DELETE FROM playlist_songs WHERE ownerPlaylistId = :playlistId AND songUniqueId = :songId")
    suspend fun deleteSongFromPlaylist(playlistId: String, songId: String)
}
