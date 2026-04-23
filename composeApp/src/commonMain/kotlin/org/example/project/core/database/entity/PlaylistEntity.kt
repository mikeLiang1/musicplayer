package org.example.project.core.database.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey val playlistId: String,
    val title: String,
    val thumbnailUrl: String?
)

@Entity(
    tableName = "playlist_songs",
    foreignKeys = [
        ForeignKey(
            entity = PlaylistEntity::class,
            parentColumns = ["playlistId"],
            childColumns = ["ownerPlaylistId"],
            onDelete = ForeignKey.CASCADE // If playlist is deleted, delete its songs too
        )
    ],
    indices = [Index(value = ["ownerPlaylistId"])]
)
data class PlaylistSongEntity(
    @PrimaryKey(autoGenerate = true) val localId: Int = 0,
    val ownerPlaylistId: String, // Links this song to a specific playlist
    val songUniqueId: String,    // The ID from your Song class
    val url: String,
    val title: String,
    val artist: String,
    val thumbnailUrl: String?,
    val duration: Long,
    val orderIndex: Int          // To keep the user's song order
)

data class PlaylistWithSongs(
    @Embedded val playlist: PlaylistEntity,
    @Relation(
        parentColumn = "playlistId",
        entityColumn = "ownerPlaylistId"
    )
    val songs: List<PlaylistSongEntity>
)
