package org.example.project.core.model.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class PlaybackStateEntity(
    @PrimaryKey val id: Int = 0,
    val currentSongUrl: String? = null,
    val positionMs: Long = 0
)
