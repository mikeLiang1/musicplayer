package org.example.project.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import org.example.project.core.database.entity.LikedSongEntity
import org.example.project.core.database.entity.PlaylistEntity
import org.example.project.core.database.entity.PlaylistSongEntity
import org.example.project.core.database.entity.PlaylistWithSongs
import org.example.project.core.database.entity.SongEntity

@Dao
interface PlaylistDao {
    // Playlists
    @Query("SELECT * FROM playlists ORDER BY updatedAt DESC")
    fun getAllPlaylists(): Flow<List<PlaylistEntity>>

    @Transaction
    @Query("SELECT * FROM playlists ORDER BY updatedAt DESC")
    fun getAllPlaylistsWithSongs(): Flow<List<PlaylistWithSongs>>

    @Transaction
    @Query("SELECT * FROM playlists WHERE id = :playlistId")
    fun getPlaylistWithSongs(playlistId: String): Flow<PlaylistWithSongs?>

    @Insert
    suspend fun insertPlaylist(playlist: PlaylistEntity)

    @Query("UPDATE playlists SET name = :name, updatedAt = :timestamp WHERE id = :id")
    suspend fun renamePlaylist(id: String, name: String, timestamp: Long)

    @Query("UPDATE playlists SET updatedAt = :timestamp WHERE id = :id")
    suspend fun touchPlaylist(id: String, timestamp: Long)

    @Query("DELETE FROM playlists WHERE id = :id")
    suspend fun deletePlaylist(id: String)

    // Songs (canonical library)
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSongIfMissing(song: SongEntity)

    @Query("SELECT * FROM songs WHERE url = :url")
    suspend fun getSong(url: String): SongEntity?

    // Liked songs
    @Query("SELECT EXISTS(SELECT 1 FROM liked_songs WHERE songUrl = :url)")
    suspend fun isSongLiked(url: String): Boolean

    @Query("SELECT EXISTS(SELECT 1 FROM liked_songs WHERE songUrl = :url)")
    fun observeIsSongLiked(url: String): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertLikedSong(likedSong: LikedSongEntity)

    @Query("DELETE FROM liked_songs WHERE songUrl = :url")
    suspend fun deleteLikedSong(url: String)

    @Query(
        "SELECT songs.* FROM songs " +
            "INNER JOIN liked_songs ON songs.url = liked_songs.songUrl " +
            "ORDER BY liked_songs.likedAt DESC"
    )
    fun getLikedSongs(): Flow<List<SongEntity>>

    @Query("SELECT COUNT(*) FROM liked_songs")
    fun getLikedSongCount(): Flow<Int>

    @Transaction
    suspend fun likeSong(song: SongEntity, likedAt: Long) {
        insertSongIfMissing(song)
        insertLikedSong(LikedSongEntity(songUrl = song.url, likedAt = likedAt))
    }

    // Playlist songs (junction)
    @Insert
    suspend fun insertPlaylistSong(playlistSong: PlaylistSongEntity)

    @Insert
    suspend fun insertPlaylistSongs(playlistSongs: List<PlaylistSongEntity>)

    @Query("DELETE FROM playlist_songs WHERE id = :id")
    suspend fun deletePlaylistSong(id: String)

    @Query("DELETE FROM playlist_songs WHERE playlistId = :playlistId")
    suspend fun deleteAllPlaylistSongs(playlistId: String)

    @Query("SELECT COALESCE(MAX(position), -1) + 1 FROM playlist_songs WHERE playlistId = :playlistId")
    suspend fun getNextPosition(playlistId: String): Int

    // Transactional helpers
    @Transaction
    suspend fun addSongToPlaylist(
        playlistId: String,
        song: SongEntity,
        timestamp: Long
    ): String {
        insertSongIfMissing(song)
        val nextPos = getNextPosition(playlistId)
        val playlistSong = PlaylistSongEntity(
            playlistId = playlistId,
            songUrl = song.url,
            position = nextPos
        )
        insertPlaylistSong(playlistSong)
        touchPlaylist(playlistId, timestamp)
        return playlistSong.id
    }

    @Transaction
    suspend fun replaceAllPlaylistSongs(
        playlistId: String,
        playlistSongs: List<PlaylistSongEntity>,
        timestamp: Long
    ) {
        deleteAllPlaylistSongs(playlistId)
        insertPlaylistSongs(playlistSongs)
        touchPlaylist(playlistId, timestamp)
    }
}

