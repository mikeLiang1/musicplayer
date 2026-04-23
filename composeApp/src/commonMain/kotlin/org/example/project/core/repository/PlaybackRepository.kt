package org.example.project.core.repository

import android.util.Log
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.example.project.core.database.MusicDatabase
import org.example.project.core.database.entity.PlaybackStateEntity
import org.example.project.core.database.mapper.toDomain
import org.example.project.core.database.mapper.toEntity
import org.example.project.core.manager.PlaybackMode
import org.example.project.core.manager.QueueState

class PlaybackRepository(
    database: MusicDatabase,
    ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val dao = database.playbackDao()

    private val scope = CoroutineScope(ioDispatcher + SupervisorJob())

    init {
        Log.d("logging", "saved data init")
        scope.launch {
            if (dao.getPlaybackStateOnce() == null) {
                dao.upsertPlaybackState(PlaybackStateEntity(id = 0))
            }
        }
    }

    suspend fun savePosition(position: Long) {
        Log.d("Logging", "saved position : $position")
        dao.updatePosition(position)
    }

    suspend fun getPosition(): Long? {
        val stateEntity = dao.getPlaybackStateOnce() ?: return null
        return stateEntity.positionMs
    }

    suspend fun saveQueueState(state: QueueState) {
        // Save base queue with type "base"
        val baseEntities = state.baseQueue.mapIndexed { index, song -> song.toEntity(index, "base") }
        // Save manual queue with type "manual"
        val manualEntities = state.manualQueue.mapIndexed { index, song -> song.toEntity(index, "manual") }
        // Save current manual song with type "current_manual" if present
        val currentManualEntity = state.currentManualSong?.toEntity(0, "current_manual")
        // Save shuffle snapshot with type "shuffle_snapshot" if present
        val snapshotEntities =
            state.preShuffleBaseQueue?.mapIndexed { index, song -> song.toEntity(index, "shuffle_snapshot") }
                ?: emptyList()

        val allEntities = baseEntities + manualEntities + snapshotEntities + listOfNotNull(currentManualEntity)

        dao.saveAllQueues(allEntities)
        dao.updatePlaybackState(
            currentIndex = state.currentBaseIndex,
            isShuffled = state.isShuffled,
            repeatMode = state.playbackMode.name,
            currentManualSongId = state.currentManualSong?.uniqueId
        )
    }

    suspend fun getQueueState(): QueueState? {
        val stateEntity = dao.getPlaybackStateOnce() ?: return null
        val baseQueue = dao.getQueueByType("base").map { it.toDomain() }
        val manualQueue = dao.getQueueByType("manual").map { it.toDomain() }
        val currentManualSongs = dao.getQueueByType("current_manual").map { it.toDomain() }
        val shuffleSnapshot = dao.getQueueByType("shuffle_snapshot").map { it.toDomain() }

        // currentManualSongs should have 0 or 1 element
        val currentManualSong = currentManualSongs.firstOrNull() ?: stateEntity.currentManualSongId?.let { id ->
            // Fallback: try to find by ID in manual or base queue (for backward compatibility)
            manualQueue.find { it.uniqueId == id } ?: baseQueue.find { it.uniqueId == id }
        }


        val queueState = QueueState(
            baseQueue = baseQueue,
            manualQueue = manualQueue,
            currentBaseIndex = stateEntity.currentIndex ?: 0,
            currentManualSong = currentManualSong,
            isShuffled = stateEntity.isShuffled,
            preShuffleBaseQueue = shuffleSnapshot.ifEmpty { null },
            preShuffleBaseIndex = if (stateEntity.isShuffled) stateEntity.currentIndex else null,
            playbackMode = PlaybackMode.valueOf(stateEntity.repeatMode ?: "OFF")
        )

        Log.d("logging", "queuestate = $queueState")
        return queueState
    }

}

