package org.example.project.core.database.mapper

import org.example.project.core.database.entity.RecentlyPlayedEntity
import org.example.project.core.model.RecentlyPlayedItem

fun RecentlyPlayedEntity.toRecentlyPlayedItem() = RecentlyPlayedItem(
    contentId = contentId,
    contentType = contentType,
    title = title,
    subTitle = subTitle,
    thumbnailUrl = thumbnailUrl,
)
