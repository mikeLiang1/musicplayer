package org.example.project.core.repository

import android.util.Log
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.example.project.core.dao.MusicDatabase
import org.example.project.core.manager.QueueState
import org.example.project.core.manager.PlaybackMode
import org.example.project.core.model.PlaybackState
import org.example.project.core.model.Song
import org.example.project.core.model.entity.PlaybackStateEntity
import org.example.project.core.model.entity.QueueEntity

class SavedDataRepository(
    database: MusicDatabase,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
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

    // The Main UI Flow: Combines Queue and State into one object
    // TODO: Remove or update for migration from old queue types ("current", "original") to new ones ("base", "manual", "shuffle_snapshot")
    suspend fun getPlaybackState(): PlaybackState? {
        val stateEntity = dao.getPlaybackStateOnce() ?: return null
        val queue = dao.getQueueOnce().map { it.toDomain() }
        val originalQueue = dao.getOriginalQueueOnce().map { it.toDomain() }

        return PlaybackState(
            queue = queue,
            originalQueue = originalQueue,
            positionMs = stateEntity.positionMs,
            currentSongId = stateEntity.currentSongId,
            index = stateEntity.currentIndex,
            isShuffled = stateEntity.isShuffled
        )
    }

    suspend fun savePosition(position: Long) {
        Log.d("Logging", "saved position : $position")
        dao.updatePosition(position)
    }

    suspend fun getPosition(): Long? {
        val stateEntity = dao.getPlaybackStateOnce() ?: return null
        return stateEntity.positionMs
    }

    suspend fun saveCurrentSongIdAndIndex(songId: String, index: Int) {
        Log.d("Logging", "saved song id :$songId at index $index")
        dao.updateCurrentSong(songId = songId, index = index)
    }

    suspend fun saveIndex(index: Int) {
        dao.updateIndex(index)
    }

    suspend fun saveIsShuffled(isShuffled: Boolean) {
        dao.updateIsShuffled(isShuffled)
    }


    // Save Queue: Map Domain to Entity and perform transaction
    suspend fun saveQueue(songs: List<Song>) {
        val entities = songs.mapIndexed { index, song ->
            song.toEntity(index, "current")
        }
        dao.saveFullQueue(entities)
    }

    suspend fun saveOriginalQueue(songs: List<Song>) {
        val entities = songs.mapIndexed { index, song ->
            song.toEntity(index, "original")
        }
        dao.saveFullOriginalQueue(entities)
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

// Mappers
fun QueueEntity.toDomain() = Song(
    uniqueId = uniqueId,
    url = url,
    title = title,
    artist = artist,
    thumbnailUrl = thumbnailUrl,
    duration = duration
)

fun Song.toEntity(index: Int, type: String) = QueueEntity(
    title = title,
    artist = artist,
    thumbnailUrl = thumbnailUrl,
    url = url,
    duration = duration,
    orderIndex = index,
    type = type,
    uniqueId = uniqueId,
    isManual = false  // Ignored for backward compatibility
)
