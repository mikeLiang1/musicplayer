package org.example.project.core.model

import org.example.project.core.database.entity.RecentlyPlayedType


data class RecentlyPlayedItem(
    val contentId: String,
    val contentType: RecentlyPlayedType,
    val title: String,
    val subTitle: String,
    val thumbnailUrl: String?,
)
