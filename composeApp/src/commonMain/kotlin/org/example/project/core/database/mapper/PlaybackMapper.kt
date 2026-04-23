package org.example.project.core.database.mapper

import org.example.project.core.database.entity.QueueEntity
import org.example.project.core.model.Song

fun QueueEntity.toDomain() = Song(
    uniqueId = uniqueId,
    url = url,
    title = title,
    artist = artist,
    thumbnailUrl = thumbnailUrl,
    duration = duration
)

fun Song.toEntity(index: Int, type: String) = QueueEntity(
    title = title,
    artist = artist,
    thumbnailUrl = thumbnailUrl,
    url = url,
    duration = duration,
    orderIndex = index,
    type = type,
    uniqueId = uniqueId,
    isManual = false  // Ignored for backward compatibility
)
