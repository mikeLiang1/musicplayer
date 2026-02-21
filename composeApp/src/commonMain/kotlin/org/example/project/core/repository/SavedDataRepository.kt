package org.example.project.core.repository

import android.util.Log
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.example.project.core.dao.MusicDatabase
import org.example.project.core.model.PlaybackState
import org.example.project.core.model.Song
import org.example.project.core.model.entity.PlaybackStateEntity
import org.example.project.core.model.entity.QueueEntity

class SavedDataRepository(database: MusicDatabase, private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO) {
    private val dao = database.playbackDao()

    private val scope = CoroutineScope(ioDispatcher + SupervisorJob())

    init {
        scope.launch {
            if (dao.getPlaybackStateOnce() == null) {
                dao.upsertPlaybackState(PlaybackStateEntity(id = 0))
            }
        }
    }

    // The Main UI Flow: Combines Queue and State into one object
    suspend fun getPlaybackState(): PlaybackState? {
        val stateEntity = dao.getPlaybackStateOnce() ?: return null
        val queue = dao.getQueueOnce().map { it.toDomain() }
        val originalQueue = dao.getOriginalQueueOnce().map { it.toDomain() }

        return PlaybackState(
            queue = queue,
            originalQueue = originalQueue,
            positionMs = stateEntity.positionMs,
            currentSongId = stateEntity.currentSongUrl,
            index = stateEntity.currentIndex,
            isShuffled = stateEntity.isShuffled
        )
    }

    suspend fun savePosition(position: Long) {
        Log.d("Logging", "saved position : $position")
        dao.updatePosition(position)
    }

    suspend fun saveCurrentSongIdAndIndex(songId: String, index: Int) {
        Log.d("Logging", "saved song id :$songId at index $index")
        dao.updateCurrentSong(songId = songId, index = index)
    }

    suspend fun saveIndex(index:Int) {
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

}

// Mappers
fun QueueEntity.toDomain() = Song(url, title, artist, thumbnailUrl, duration)
fun Song.toEntity(index: Int, type: String) = QueueEntity(
    title = title, artist = artist,
    thumbnailUrl = thumbnailUrl, url = url, duration = duration, orderIndex = index, type = type
)
