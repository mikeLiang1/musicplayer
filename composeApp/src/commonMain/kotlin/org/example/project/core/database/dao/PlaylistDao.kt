package org.example.project.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
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
    suspend fun deleteSongsByPlaylist(playlistId: String)

    @Query("DELETE FROM playlists WHERE playlistId = :playlistId")
    suspend fun deletePlaylistOnly(playlistId: String)

    // Get all playlists (Flow updates automatically when data changes)
    @Transaction
    @Query("SELECT * FROM playlists")
    fun getAllPlaylistsWithSongs(): kotlinx.coroutines.flow.Flow<List<PlaylistWithSongs>>

    // High-level function to save/update a whole playlist safely
    @Transaction
    suspend fun saveFullPlaylist(playlist: PlaylistEntity, songs: List<PlaylistSongEntity>) {
        insertPlaylist(playlist)
        // We delete old versions of songs for this playlist to avoid duplicates/order issues
        deleteSongsByPlaylist(playlist.playlistId)
        insertSongs(songs)
    }
}
