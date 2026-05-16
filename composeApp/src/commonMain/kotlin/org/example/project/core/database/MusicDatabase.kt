package org.example.project.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import org.example.project.core.database.dao.PlaybackDao
import org.example.project.core.database.dao.PlaylistDao
import org.example.project.core.database.dao.RecentlyPlayedDao
import org.example.project.core.database.entity.PlaybackStateEntity
import org.example.project.core.database.entity.PlaylistEntity
import org.example.project.core.database.entity.PlaylistSongEntity
import org.example.project.core.database.entity.QueueEntity
import org.example.project.core.database.entity.RecentlyPlayedEntity
import org.example.project.core.database.entity.SongEntity

@TypeConverters(Converters::class)
@Database(
    entities = [
        QueueEntity::class, PlaybackStateEntity::class, PlaylistEntity::class,
        PlaylistSongEntity::class, SongEntity::class, RecentlyPlayedEntity::class
    ],
    version = 5
)
abstract class MusicDatabase : RoomDatabase() {
    abstract fun playbackDao(): PlaybackDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun recentlyPlayedDao(): RecentlyPlayedDao
}

// We need this to instantiate the DB on iOS
fun getRoomDatabase(builder: RoomDatabase.Builder<MusicDatabase>): MusicDatabase {
    return builder
        .setDriver(BundledSQLiteDriver()) // Use the bundled driver for KMP
        .setQueryCoroutineContext(Dispatchers.IO)
        .fallbackToDestructiveMigration(true)
        .build()
}
