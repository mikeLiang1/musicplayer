package org.example.project.core.dao

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import org.example.project.core.model.entity.PlaybackStateEntity
import org.example.project.core.model.entity.QueueEntity

@Database(entities = [QueueEntity::class, PlaybackStateEntity::class], version = 1)
abstract class MusicDatabase : RoomDatabase() {
    abstract fun playbackDao(): PlaybackDao
}

// We need this to instantiate the DB on iOS
fun getRoomDatabase(builder: RoomDatabase.Builder<MusicDatabase>): MusicDatabase {
    return builder
        .setDriver(BundledSQLiteDriver()) // Use the bundled driver for KMP
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()
}
