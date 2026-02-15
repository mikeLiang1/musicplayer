package org.example.project.core.model.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class QueueEntity(
    @PrimaryKey(autoGenerate = true) val autoId: Int = 0,
    val title: String,
    val artist: String,
    val thumbnailUrl: String?,
    val url: String,
    val duration: Long,
    val orderIndex: Int // Important for maintaining list order
)
