package org.example.project.core.database

import androidx.room.TypeConverter
import org.example.project.core.database.entity.RecentlyPlayedType

class Converters {
    @TypeConverter
    fun fromRecentlyPlayedType(type: RecentlyPlayedType): String = type.name

    @TypeConverter
    fun toRecentlyPlayedType(value: String): RecentlyPlayedType =
        RecentlyPlayedType.valueOf(value)
}
