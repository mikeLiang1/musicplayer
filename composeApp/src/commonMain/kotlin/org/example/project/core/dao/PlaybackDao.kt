package org.example.project.core.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import org.example.project.core.model.entity.PlaybackStateEntity
import org.example.project.core.model.entity.QueueEntity

@Dao
interface PlaybackDao {
    // --- Queue Logic ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQueue(songs: List<QueueEntity>)

    @Transaction
    suspend fun saveFullQueue(songs: List<QueueEntity>) {
        clearQueue()
        insertQueue(songs)
    }

    @Transaction
    suspend fun saveFullOriginalQueue(songs: List<QueueEntity>) {
        clearOriginalQueue()
        insertQueue(songs)
    }

    @Query("SELECT * FROM QueueEntity WHERE type = 'current' ORDER BY orderIndex ASC")
    suspend fun getQueueOnce(): List<QueueEntity>

    @Query("SELECT * FROM QueueEntity WHERE type = 'original' ORDER BY orderIndex ASC")
    suspend fun getOriginalQueueOnce(): List<QueueEntity>

    @Query("DELETE FROM QueueEntity WHERE type = 'current'")
    suspend fun clearQueue()

    @Query("DELETE FROM QueueEntity WHERE type = 'original'")
    suspend fun clearOriginalQueue()


    @Query("SELECT * FROM PlaybackStateEntity WHERE id = 0")
    suspend fun getPlaybackStateOnce(): PlaybackStateEntity?

    @Upsert
    suspend fun upsertPlaybackState(state: PlaybackStateEntity)


    @Query("UPDATE PlaybackStateEntity SET positionMs = :position WHERE id = 0")
    suspend fun updatePosition(position: Long)

    @Query("UPDATE PlaybackStateEntity SET currentSongUrl = :songId, currentIndex = :index, positionMs = 0 WHERE id = 0")
    suspend fun updateCurrentSong(songId: String, index: Int)

    @Query("UPDATE PlaybackStateEntity SET currentIndex = :index WHERE id = 0")
    suspend fun updateIndex(index: Int)

    @Query("UPDATE PlaybackStateEntity SET isShuffled = :isShuffled WHERE id = 0")
    suspend fun updateIsShuffled(isShuffled: Boolean)
}
