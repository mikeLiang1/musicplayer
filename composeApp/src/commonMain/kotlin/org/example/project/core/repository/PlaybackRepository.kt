package org.example.project.core.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.example.project.core.database.dao.PlaybackDao
import org.example.project.core.database.entity.PlaybackStateEntity
import org.example.project.core.database.mapper.toDomain
import org.example.project.core.database.mapper.toEntity
import org.example.project.core.manager.PlaybackMode
import org.example.project.core.manager.QueueState

class PlaybackRepository(
    private val dao: PlaybackDao,
    ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val scope = CoroutineScope(ioDispatcher + SupervisorJob())

    // Signature of the last-persisted queue contents, used to skip redundant
    // full-table rewrites when only the playback pointers (index/shuffle/repeat) change.
    private var lastQueueSignature: Int? = null

    init {
        scope.launch {
            if (dao.getPlaybackStateOnce() == null) {
                dao.upsertPlaybackState(PlaybackStateEntity(id = 0))
            }
        }
    }

    suspend fun savePosition(position: Long) {
        dao.updatePosition(position)
    }

    suspend fun saveQueueState(state: QueueState) {
        // Only rewrite the (potentially large) queue rows when the song contents actually
        // change. The common case during playback is just the current index advancing, which
        // needs nothing more than a cheap PlaybackStateEntity update.
        val signature = queueSignature(state)
        if (signature != lastQueueSignature) {
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
            lastQueueSignature = signature
        }

        dao.updatePlaybackState(
            currentIndex = state.currentBaseIndex,
            isShuffled = state.isShuffled,
            repeatMode = state.playbackMode.name,
            currentManualSongId = state.currentManualSong?.uniqueId
        )
    }

    suspend fun getRestoredPlayback(): RestoredPlayback? {
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
            // preShuffleBaseIndex is intentionally null: it is not persisted, and unshuffle()
            // recomputes the index from the snapshot, so storing the shuffled index here was wrong.
            preShuffleBaseIndex = null,
            playbackMode = PlaybackMode.valueOf(stateEntity.repeatMode ?: "OFF")
        )

        // Prime the signature so the first post-restore save doesn't needlessly rewrite the rows
        // we just read back.
        lastQueueSignature = queueSignature(queueState)

        return RestoredPlayback(queueState, stateEntity.positionMs)
    }

    private fun queueSignature(state: QueueState): Int = listOf(
        state.baseQueue.map { it.uniqueId },
        state.manualQueue.map { it.uniqueId },
        state.preShuffleBaseQueue?.map { it.uniqueId },
        state.currentManualSong?.uniqueId
    ).hashCode()
}

data class RestoredPlayback(
    val queueState: QueueState,
    val positionMs: Long
)

