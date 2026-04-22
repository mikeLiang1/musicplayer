package org.example.project.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import org.example.project.core.database.entity.PlaybackStateEntity
import org.example.project.core.database.entity.QueueEntity

@Dao
interface PlaybackDao {
    // --- Queue Logic ---

    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
    suspend fun insertQueue(songs: List<QueueEntity>)



    @Query("SELECT * FROM PlaybackStateEntity WHERE id = 0")
    suspend fun getPlaybackStateOnce(): PlaybackStateEntity?

    @Upsert
    suspend fun upsertPlaybackState(state: PlaybackStateEntity)


    @Query("UPDATE PlaybackStateEntity SET positionMs = :position WHERE id = 0")
    suspend fun updatePosition(position: Long)


    // --- New methods for QueueState persistence ---

    @Transaction
    suspend fun saveAllQueues(entities: List<QueueEntity>) {
        // Clear all queue types first
        clearAllQueues()
        insertQueue(entities)
    }

    @Query("DELETE FROM QueueEntity WHERE type IN ('base', 'manual', 'shuffle_snapshot', 'current_manual')")
    suspend fun clearAllQueues()

    @Query("SELECT * FROM QueueEntity WHERE type = :type ORDER BY orderIndex ASC")
    suspend fun getQueueByType(type: String): List<QueueEntity>

    @Query("""
        UPDATE PlaybackStateEntity
        SET currentIndex = :currentIndex,
            isShuffled = :isShuffled,
            repeatMode = :repeatMode,
            currentManualSongId = :currentManualSongId
        WHERE id = 0
    """)
    suspend fun updatePlaybackState(
        currentIndex: Int?,
        isShuffled: Boolean,
        repeatMode: String?,
        currentManualSongId: String?
    )
}
