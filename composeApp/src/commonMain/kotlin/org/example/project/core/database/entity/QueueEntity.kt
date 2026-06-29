package org.example.project.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class QueueEntity(
    @PrimaryKey(autoGenerate = true) val autoId: Int = 0,
    val uniqueId: String,
    val title: String,
    val artist: String,
    val thumbnailUrl: String?,
    val url: String,
    val duration: Long,
    val type: String, // "base", "manual", "current_manual", or "shuffle_snapshot"
    val isManual: Boolean, // unused; distinction is carried by `type`
    val orderIndex: Int // Important for maintaining list order
)
