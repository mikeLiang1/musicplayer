package org.example.project.core.repository

import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import org.example.project.core.dao.MusicDatabase
import org.example.project.core.model.PlaybackState
import org.example.project.core.model.Song
import org.example.project.core.model.entity.PlaybackStateEntity
import org.example.project.core.model.entity.QueueEntity

class PlaybackRepository(private val database: MusicDatabase) {
    private val dao = database.playbackDao()

    // The Main UI Flow: Combines Queue and State into one object
    val playbackState: Flow<PlaybackState> = combine(
        dao.getQueueFlow(),
        dao.getPlaybackStateFlow()
    ) { queueEntities, stateEntity ->
        val songs = queueEntities.map { it.toDomain() }
        PlaybackState(
            queue = songs,
            positionMs = stateEntity?.positionMs ?: 0L,
            currentSongId = stateEntity?.currentSongUrl // This is your "pointer"
        )
    }

    // Save position: Fetch current row, update time, save back
    suspend fun savePosition(position: Long) {
        Log.d("Logging", "saved position : $position")
        val current = dao.getPlaybackStateOnce() ?: PlaybackStateEntity(id = 0)
        dao.upsertPlaybackState(
            current.copy(positionMs = position)
        )
    }

    // Save Song Change: Reset position to 0 and update ID
    suspend fun saveCurrentSongId(songId: String) {
        Log.d("Logging", "saved song id :$songId")
        dao.upsertPlaybackState(
            PlaybackStateEntity(
                id = 0,
                currentSongUrl = songId,
                positionMs = 0L // New song starts at the beginning
            )
        )
    }

    // Save Queue: Map Domain to Entity and perform transaction
    suspend fun saveQueue(songs: List<Song>) {
        val entities = songs.mapIndexed { index, song ->
            song.toEntity(index)
        }
        dao.saveFullQueue(entities)
    }
}

// Mappers
fun QueueEntity.toDomain() = Song(url, title, artist, thumbnailUrl, duration)
fun Song.toEntity(index: Int) = QueueEntity(
    title = title, artist = artist,
    thumbnailUrl = thumbnailUrl, url = url, duration = duration, orderIndex = index
)
