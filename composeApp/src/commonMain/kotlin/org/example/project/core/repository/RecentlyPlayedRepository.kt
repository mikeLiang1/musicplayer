package org.example.project.core.repository

import kotlinx.coroutines.flow.Flow
import org.example.project.core.database.MusicDatabase
import org.example.project.core.database.entity.RecentlyPlayedEntity
import org.example.project.core.database.entity.RecentlyPlayedType
import org.example.project.core.model.Playlist
import org.example.project.core.model.Song

class RecentlyPlayedRepository(database: MusicDatabase) {
    private val dao = database.recentlyPlayedDao()

    val recentlyPlayed: Flow<List<RecentlyPlayedEntity>> =
        dao.getRecentlyPlayed()

    suspend fun recordSong(song: Song) {
        dao.upsert(
            RecentlyPlayedEntity(
                contentId = song.url,
                contentType = RecentlyPlayedType.SONG,
                title = song.title,
                subTitle = song.artist,
                thumbnailUrl = song.thumbnailUrl,
                playedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun recordPlaylist(playlist: Playlist) {
        dao.upsert(
            RecentlyPlayedEntity(
                contentId = playlist.id,
                contentType = RecentlyPlayedType.PLAYLIST,
                title = playlist.name,
                subTitle = "Playlist",
                thumbnailUrl = playlist.thumbnailUrl,
                playedAt = System.currentTimeMillis()
            )
        )
    }
}
