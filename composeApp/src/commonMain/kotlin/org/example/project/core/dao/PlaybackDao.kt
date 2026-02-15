package org.example.project.core.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import org.example.project.core.model.entity.PlaybackStateEntity
import org.example.project.core.model.entity.QueueEntity

@Dao
interface PlaybackDao {
    // --- Queue Logic ---
    @Query("SELECT * FROM QueueEntity ORDER BY orderIndex ASC")
    fun getQueueFlow(): Flow<List<QueueEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQueue(songs: List<QueueEntity>)

    @Query("DELETE FROM QueueEntity")
    suspend fun clearQueue()

    @Transaction
    suspend fun saveFullQueue(songs: List<QueueEntity>) {
        clearQueue()
        insertQueue(songs)
    }

    // --- Playback State Logic ---
    @Query("SELECT * FROM PlaybackStateEntity WHERE id = 0")
    fun getPlaybackStateFlow(): Flow<PlaybackStateEntity?>

    @Query("SELECT * FROM PlaybackStateEntity WHERE id = 0")
    suspend fun getPlaybackStateOnce(): PlaybackStateEntity?

    @Upsert
    suspend fun upsertPlaybackState(state: PlaybackStateEntity)
}
