package org.example.project.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class PlaybackStateEntity(
    @PrimaryKey val id: Int = 0,
    val currentSongId: String? = null,
    val positionMs: Long = 0,
    val currentIndex: Int? = null,
    val isShuffled: Boolean = false,
    val repeatMode: String? = null,
    val currentManualSongId: String? = null,
    // Where the saved queue came from (QueueContext). Nullable throughout: rows written before
    // v9, and queues with no source, simply have no context to restore.
    val contextId: String? = null,
    val contextType: String? = null,
    val contextTitle: String? = null
)
