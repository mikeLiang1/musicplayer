package org.example.project.core.database.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

import androidx.room.*
import java.util.UUID
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@Entity(tableName = "songs")
data class SongEntity(
    @PrimaryKey val url: String,
    val title: String,
    val artist: String,
    val thumbnailUrl: String?,
    val duration: Long,
    val firstAddedAt: Long
)
// Playlist metadata — one row per playlist
@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey val id: String,
    val name: String,
    val createdAt: Long,
    val updatedAt: Long,
    val thumbnailUrl: String
)
// Junction table — links playlists :left_right_arrow: songs with ordering
@OptIn(ExperimentalUuidApi::class)
@Entity(
    tableName = "playlist_songs",
    foreignKeys = [
        ForeignKey(
            entity = PlaylistEntity::class,
            parentColumns = ["id"],
            childColumns = ["playlistId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = SongEntity::class,
            parentColumns = ["url"],
            childColumns = ["songUrl"]
        )
    ],
    indices = [
        Index("playlistId"),
        Index("songUrl")
    ]
)
data class PlaylistSongEntity(
    @PrimaryKey val id: String = Uuid.random().toString(),
    val playlistId: String,
    val songUrl: String,
    val position: Int
)


// One playlist song row + its joined song record
data class PlaylistSongWithSong(
    @Embedded val playlistSong: PlaylistSongEntity,
    @Relation(
        parentColumn = "songUrl",
        entityColumn = "url"
    )
    val song: SongEntity
)
// A playlist + all its songs (each with full song metadata via the join above)
data class PlaylistWithSongs(
    @Embedded val playlist: PlaylistEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "playlistId",
        entity = PlaylistSongEntity::class
    )
    val songs: List<PlaylistSongWithSong>
)
