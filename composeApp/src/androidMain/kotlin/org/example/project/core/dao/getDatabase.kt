package org.example.project.core.dao

import androidx.room.Room
import androidx.room.RoomDatabase
import coil3.PlatformContext
import org.example.project.core.database.MusicDatabase

fun getDatabaseBuilder(ctx: PlatformContext): RoomDatabase.Builder<MusicDatabase> {
    val appContext = ctx.applicationContext
    val dbFile = appContext.getDatabasePath("music.db")
    return Room.databaseBuilder<MusicDatabase>(
        context = appContext,
        name = dbFile.absolutePath
    )
}
