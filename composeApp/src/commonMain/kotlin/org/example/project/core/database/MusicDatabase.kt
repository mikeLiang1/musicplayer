package org.example.project.core.database

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import org.example.project.core.database.dao.PlaybackDao
import org.example.project.core.database.dao.PlaylistDao
import org.example.project.core.database.dao.RecentlyPlayedDao
import org.example.project.core.database.entity.LikedSongEntity
import org.example.project.core.database.entity.PlaybackStateEntity
import org.example.project.core.database.entity.PlaylistEntity
import org.example.project.core.database.entity.PlaylistSongEntity
import org.example.project.core.database.entity.QueueEntity
import org.example.project.core.database.entity.RecentlyPlayedEntity
import org.example.project.core.database.entity.SongEntity

/**
 * Schema changes from version 7 onward MUST preserve user data.
 *
 * To change the schema:
 *  1. Edit/add the entity (and register new entities in [entities] below).
 *  2. Bump [version] by one.
 *  3. Add an `AutoMigration(from = <old>, to = <new>)` entry to [autoMigrations].
 *  4. Build once so KSP writes `composeApp/schemas/.../<new>.json`, and commit that file.
 *     The exported JSONs are the input Room diffs to generate the migration — losing
 *     them means the migration can no longer be generated.
 *
 * Room auto-generates ADD COLUMN / CREATE TABLE / index changes on its own, but a new
 * non-null column needs a SQL-level default — a Kotlin default value is not enough, and
 * the build fails with "New NOT NULL column 'x' added with no default value specified":
 *
 *     @ColumnInfo(defaultValue = "") val description: String = ""
 *     @ColumnInfo(defaultValue = "0") val playCount: Int = 0
 *
 * Destructive edits need a hint too, otherwise the build fails with "Column/Table ... was removed":
 *  - dropping a column   -> `@DeleteColumn(tableName = "x", columnName = "y")`
 *  - renaming a column   -> `@RenameColumn(tableName = "x", fromColumnName = "a", toColumnName = "b")`
 *  - dropping/renaming a table -> `@DeleteTable` / `@RenameTable`
 * Those annotations go on an `AutoMigrationSpec` class passed as
 * `AutoMigration(from = 8, to = 9, spec = MySpec::class)`.
 *
 * If a change is too complex for Room to generate (splitting a table, back-filling
 * derived values), write a `Migration(from, to)` by hand and pass it to
 * `addMigrations(...)` in [getRoomDatabase] instead of adding an `AutoMigration`.
 */
@TypeConverters(Converters::class)
@Database(
    entities = [
        QueueEntity::class, PlaybackStateEntity::class, PlaylistEntity::class,
        PlaylistSongEntity::class, SongEntity::class, RecentlyPlayedEntity::class,
        LikedSongEntity::class
    ],
    version = 8,
    exportSchema = true,
    autoMigrations = [
        // Append one entry per version bump.
        AutoMigration(from = 7, to = 8), // playlists.lastPlayedAt
    ]
)
abstract class MusicDatabase : RoomDatabase() {
    abstract fun playbackDao(): PlaybackDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun recentlyPlayedDao(): RecentlyPlayedDao
}

/**
 * Versions 1-6 predate the migration policy: no schema was ever exported for 6, and no
 * migrations were written for 1-5, so those installs are wiped on upgrade. That list is
 * closed — never add 7 or higher to it.
 *
 * There is deliberately no blanket `fallbackToDestructiveMigration`. If a version bump
 * ships without a matching migration, Room throws on first open ("A migration from N to M
 * was required but not found") instead of silently deleting every playlist, liked song and
 * saved queue. A crash in dev is recoverable; a wipe on a user's device is not.
 */
private val DESTRUCTIVELY_MIGRATED_LEGACY_VERSIONS = intArrayOf(1, 2, 3, 4, 5, 6)

// We need this to instantiate the DB on iOS
fun getRoomDatabase(builder: RoomDatabase.Builder<MusicDatabase>): MusicDatabase {
    return builder
        .setDriver(BundledSQLiteDriver()) // Use the bundled driver for KMP
        .setQueryCoroutineContext(Dispatchers.IO)
        .fallbackToDestructiveMigrationFrom(
            dropAllTables = true,
            *DESTRUCTIVELY_MIGRATED_LEGACY_VERSIONS
        )
        // Checking out an older branch opens a newer DB file; recreate instead of crashing.
        .fallbackToDestructiveMigrationOnDowngrade(dropAllTables = true)
        .build()
}
