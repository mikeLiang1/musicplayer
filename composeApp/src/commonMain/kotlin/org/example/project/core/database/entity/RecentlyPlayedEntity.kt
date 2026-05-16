package org.example.project.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class RecentlyPlayedType { SONG, PLAYLIST }

@Entity(tableName = "recently_played")
data class RecentlyPlayedEntity(
    @PrimaryKey val contentId: String,
    val contentType: RecentlyPlayedType,
    val title: String,
    val subTitle: String,
    val thumbnailUrl: String?,
    val playedAt: Long,
)
