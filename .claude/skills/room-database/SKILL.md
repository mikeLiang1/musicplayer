---
name: room-database
description: Use when adding or changing anything in the Room database layer — entities, DAOs, mappers, database version, converters, or DataStore preferences.
---

# Room Database Changes (KMP)

Room 2.8.4 in KMP mode. All database code lives in `commonMain`; only the platform builder lives in `androidMain`. Read this whole file before touching schema.

## Where everything lives

| Piece | Path |
|---|---|
| `@Database` class (version, entity list) | `composeApp/src/commonMain/kotlin/org/example/project/core/database/MusicDatabase.kt` |
| Driver + migration policy (`getRoomDatabase`) | same file, bottom function |
| Android builder (`getDatabaseBuilder`) | `composeApp/src/androidMain/kotlin/org/example/project/core/dao/getDatabase.kt` (note: package `core.dao`, not `core.database`) |
| Type converters | `composeApp/src/commonMain/kotlin/org/example/project/core/database/Converters.kt` |
| Entities | `composeApp/src/commonMain/kotlin/org/example/project/core/database/entity/` |
| DAOs | `composeApp/src/commonMain/kotlin/org/example/project/core/database/dao/` |
| Mappers (entity ↔ domain) | `composeApp/src/commonMain/kotlin/org/example/project/core/database/mapper/` |
| Koin wiring | `composeApp/src/commonMain/kotlin/org/example/project/core/di/AppModule.kt` (`databaseModule`, `repositoryModule`) and `composeApp/src/androidMain/kotlin/org/example/project/core/di/AndroidModule.kt` |
| Schema JSONs | `composeApp/schemas/org.example.project.core.database.MusicDatabase/1.json` … `5.json` |
| Gradle (KSP, room plugin) | `composeApp/build.gradle.kts` |

## Current schema (version 7)

Entities registered in `MusicDatabase.kt`:

```kotlin
@Database(
    entities = [
        QueueEntity::class, PlaybackStateEntity::class, PlaylistEntity::class,
        PlaylistSongEntity::class, SongEntity::class, RecentlyPlayedEntity::class,
        LikedSongEntity::class
    ],
    version = 7,
    exportSchema = true,
    autoMigrations = [ /* append one entry per version bump */ ]
)
```

| Entity (file) | Table name | Primary key | Notable columns / relationships |
|---|---|---|---|
| `QueueEntity` (QueueEntity.kt) | `QueueEntity` (default) | `autoId: Int` autoGenerate | `type: String` discriminates rows: `"base"`, `"manual"`, `"current_manual"`, `"shuffle_snapshot"`. `orderIndex` preserves order. `isManual` is dead ("unused; distinction is carried by `type`"). One table stores all four queues. |
| `PlaybackStateEntity` (PlaybackEntity.kt) | `PlaybackStateEntity` (default) | `id: Int = 0` | Singleton row (always id 0): `currentSongId`, `positionMs`, `currentIndex`, `isShuffled`, `repeatMode`, `currentManualSongId` |
| `PlaylistEntity` (PlaylistEntity.kt) | `playlists` | `id: String` (UUID) | `name`, `createdAt`, `updatedAt`, `thumbnailUrl`. Sorted by `updatedAt DESC`. |
| `PlaylistSongEntity` (PlaylistEntity.kt) | `playlist_songs` | `id: String` (UUID default) | Junction table. FK `playlistId` → `playlists.id` with `onDelete = CASCADE`; FK `songUrl` → `songs.url` (no cascade). Indices on both FK columns. `position: Int` for ordering. |
| `SongEntity` (PlaylistEntity.kt) | `songs` | `url: String` | Canonical song library shared by all playlists: `title`, `artist`, `thumbnailUrl`, `duration`, `firstAddedAt` |
| `RecentlyPlayedEntity` (RecentlyPlayedEntity.kt) | `recently_played` | `contentId: String` | `contentType: RecentlyPlayedType` (SONG/PLAYLIST enum via converter), `playedAt` used for `ORDER BY ... DESC LIMIT 20` |

Relationships are NOT foreign objects at query time except for playlists: `PlaylistWithSongs` (`@Embedded PlaylistEntity` + `@Relation` to `PlaylistSongEntity`) and `PlaylistSongWithSong` (`@Embedded PlaylistSongEntity` + `@Relation` to `SongEntity`), both defined in `PlaylistEntity.kt`. No embedded lists via converters — the only converter is `RecentlyPlayedType` ↔ `String` in `Converters.kt`.

## Migration policy — THE TRUTH

Migration policy is configured in **commonMain**, in `getRoomDatabase()` at the bottom of `MusicDatabase.kt` (not in the Android builder — `getDatabase.kt` only supplies the file path `music.db`):

```kotlin
return builder
    .setDriver(BundledSQLiteDriver()) // Use the bundled driver for KMP
    .setQueryCoroutineContext(Dispatchers.IO)
    .fallbackToDestructiveMigrationFrom(dropAllTables = true, 1, 2, 3, 4, 5, 6)
    .fallbackToDestructiveMigrationOnDowngrade(dropAllTables = true)
    .build()
```

**From version 7 onward, schema changes preserve user data via Room auto-migrations.** There is deliberately **no blanket `fallbackToDestructiveMigration`**.

- Versions **1–6 are legacy**: no migrations were ever written for them and no schema was exported for 6, so those installs are still wiped on upgrade. **That list is closed — never add 7 or higher to it.**
- If you bump the version and forget the migration, Room throws on first open (`A migration from N to M was required but not found`) instead of silently deleting every playlist, liked song and saved queue. A crash in dev is recoverable; a wipe on a user's device is not. Do not "fix" that crash by re-adding a blanket destructive fallback — add the missing `AutoMigration`.
- Downgrades (checking out an older branch against a newer DB file) still recreate the DB rather than crashing.
- The historical schema JSONs in `composeApp/schemas/` are the **input** Room diffs to generate migrations. Losing one means migrations from that version can no longer be generated — always commit the new JSON.

### Writing the migration

Most changes need only an entry in `autoMigrations`; Room generates the SQL:

```kotlin
autoMigrations = [
    AutoMigration(from = 7, to = 8),
]
```

Room handles ADD COLUMN, CREATE TABLE and index changes by itself. Two cases need help:

**New non-null column** — a Kotlin default is *not* a SQL default. Without `@ColumnInfo(defaultValue = ...)` the build fails with `New NOT NULL column 'x' added with no default value specified`:

```kotlin
@ColumnInfo(defaultValue = "") val description: String = ""
@ColumnInfo(defaultValue = "0") val playCount: Int = 0
```

**Dropping/renaming** — fails the build with `Column/Table ... was removed` unless you supply an `AutoMigrationSpec`:

```kotlin
@DeleteColumn(tableName = "playlists", columnName = "thumbnailUrl")
class Migration8To9 : AutoMigrationSpec

autoMigrations = [AutoMigration(from = 8, to = 9, spec = Migration8To9::class)]
```

Other spec annotations: `@RenameColumn(tableName, fromColumnName, toColumnName)`, `@DeleteTable(tableName)`, `@RenameTable(fromTableName, toTableName)`.

**Too complex to generate** (splitting a table, back-filling derived values): write a `Migration(from, to)` by hand and pass it to `addMigrations(...)` in `getRoomDatabase()` instead of adding an `AutoMigration`.

## Recipe: add a column (or a new entity) end-to-end

1. **Entity** — edit/create the data class in `core/database/entity/`. A new non-null column needs **both** a Kotlin default and `@ColumnInfo(defaultValue = "...")` (the SQL default the auto-migration emits — see migration policy). For a new entity, set `tableName` explicitly (the older entities that didn't are stuck with class-name tables).
2. **Register** — new entities must be added to the `entities = [...]` list in `MusicDatabase.kt`.
3. **DAO** — add queries in the matching DAO in `core/database/dao/`. Follow the existing conventions:
   - Reactive reads return `Flow<...>` (see `PlaylistDao.getAllPlaylistsWithSongs()`, `RecentlyPlayedDao.getRecentlyPlayed()`); one-shot reads/writes are `suspend fun`.
   - Multi-statement operations go in `@Transaction suspend fun` default methods on the DAO interface (see `PlaylistDao.addSongToPlaylist`, `PlaybackDao.saveAllQueues`).
   - Queries returning `PlaylistWithSongs` need `@Transaction` on the `@Query` too.
   - Upsert styles in use: `@Upsert` (PlaybackDao), `@Insert(onConflict = OnConflictStrategy.REPLACE)` (RecentlyPlayedDao), `@Insert(onConflict = OnConflictStrategy.IGNORE)` (`insertSongIfMissing`).
   - A brand-new DAO also needs an `abstract fun myDao(): MyDao` in `MusicDatabase`.
4. **Mapper** — add `toDomain()` / `toEntity()` extensions in `core/database/mapper/`. Domain models live in `core/model/`. Note the pattern in `PlaylistMapper.kt`: `SongEntity.toSong(idOverride)` injects the persistent `playlist_songs.id` as the domain `Song.uniqueId`.
5. **Repository** — consume the DAO from a repository, not from ViewModels directly. Existing patterns: `RecentlyPlayedRepository` takes `MusicDatabase` and calls `database.playlistDao()` itself; `PlaybackRepository` and `PlaylistRepository` both take their DAO directly (injected in `AppModule.kt` as `PlaybackRepository(get<MusicDatabase>().playbackDao())` / `PlaylistRepository(get<MusicDatabase>().playlistDao())`). **Prefer taking the DAO directly** — it's what makes the repository unit-testable with a hand-written fake DAO (see `PlaylistRepositoryTest.kt` / `PlaybackRepositoryTest.kt`) without needing a fake `MusicDatabase`/`RoomDatabase`.
6. **Koin** — if you created a new repository, register it in `repositoryModule` in `core/di/AppModule.kt` (`single { MyRepository(get()) }`). The database itself is already provided: `databaseModule` has `single<MusicDatabase> { getRoomDatabase(get()) }`, and the `RoomDatabase.Builder` comes from `androidModule` in `AndroidModule.kt`. New Koin modules must also be added to `startKoin { modules(...) }` in `androidMain/MainApplication.kt` — but adding to an existing module needs nothing extra.
7. **Bump version** — `version = 7` → `8` in the `@Database` annotation. Skipping this on an already-installed app crashes with an integrity error (see failures table).
8. **Add the migration** — append `AutoMigration(from = 7, to = 8)` to `autoMigrations` in the same annotation. Skipping this crashes on first open with `A migration from 7 to 8 was required but not found`. See the migration policy section for `@ColumnInfo(defaultValue = ...)` and `AutoMigrationSpec` cases.
9. **Rebuild** — `./gradlew :composeApp:assembleDebug`. KSP regenerates the DAO/database impls, generates `MusicDatabase_AutoMigration_7_8_Impl`, and writes `composeApp/schemas/org.example.project.core.database.MusicDatabase/8.json`. **Commit that JSON** — future migrations diff against it.
10. **Verify** — install *over* the previous build (don't uninstall) and confirm existing playlists, liked songs and the saved queue survived. Uninstalling first hides exactly the bug you're checking for.

## KSP / Gradle gotchas

- BOTH compiler entries in `composeApp/build.gradle.kts` are required; if you touch them keep both:
  ```kotlin
  add("kspCommonMainMetadata", "androidx.room:room-compiler:2.8.4")
  add("kspAndroid", "androidx.room:room-compiler:2.8.4")
  ```
  Missing `kspAndroid` → "cannot find implementation for MusicDatabase" at runtime. Missing `kspCommonMainMetadata` → unresolved generated symbols when compiling common metadata.
- `room { schemaDirectory("$projectDir/schemas") }` sits OUTSIDE the `android {}` block (comment in the file says MUST).
- `composeApp/schemas/` currently contains versions 1–5 and 7 under `org.example.project.core.database.MusicDatabase/` (**6 was never exported** — that gap is why 6 sits in the destructive-fallback list and can never be auto-migrated from), plus a stale leftover directory `org.example.project.core.dao.MusicDatabase/1.json` from before the class moved packages — ignore it, don't "fix" it.
- Deleting or failing to commit a schema JSON permanently breaks auto-migration from that version. Treat `composeApp/schemas/` as append-only.
- Room runtime deps in commonMain are `libs.androidx.room.runtime` + `libs.sqlite.bundled` (the `BundledSQLiteDriver`). Don't add the Android-only `room-ktx`.

## DataStore (preferences)

`core/helper/DataStoreHelper.kt` only defines the factory and file name:

```kotlin
internal const val dataStoreFileName = "dice.preferences_pb"
fun createDataStore(producePath: () -> String): DataStore<Preferences> = ...
```

It is provided in Koin (`AndroidModule.kt`: `single<DataStore<Preferences>> { createDataStore { androidContext().filesDir.resolve(dataStoreFileName).absolutePath } }`) but **no preference keys exist and nothing injects it yet** — it is wired-up-but-unused scaffolding. To add a preference: inject `DataStore<Preferences>` via Koin, define a key (`val myKey = booleanPreferencesKey("my_key")` from `androidx.datastore.preferences.core`), read via `dataStore.data.map { it[myKey] ?: default }`, write via `dataStore.edit { it[myKey] = value }`. Don't rename `dataStoreFileName` (would orphan any existing file).

## Common failures

| Symptom | Cause | Fix |
|---|---|---|
| Crash on launch: "Room cannot verify the data integrity. Looks like you've changed schema but forgot to update the version number" | Entity changed, `version` in `MusicDatabase.kt` not bumped | Bump `version`; during development you can also clear app data / uninstall |
| Crash on launch: "A migration from 7 to 8 was required but not found" | Version bumped without adding the matching `AutoMigration` entry | Add `AutoMigration(from = 7, to = 8)` to `autoMigrations`. **Do not** re-add a blanket `fallbackToDestructiveMigration` — that trades a dev crash for silent user data loss |
| Build error: "New NOT NULL column 'x' added with no default value specified" | Auto-migration can't backfill existing rows; a Kotlin default is not a SQL default | Add `@ColumnInfo(defaultValue = "...")` to the field (or make it nullable) |
| Build error: "Column/Table 'x' was removed/renamed" | Auto-migration can't guess intent for destructive edits | Supply an `AutoMigrationSpec` with `@DeleteColumn` / `@RenameColumn` / `@DeleteTable` / `@RenameTable` and pass it as `AutoMigration(..., spec = ...)` |
| Build error: "Cannot find the schema file for version N" | Schema JSON for the `from` version missing from `composeApp/schemas/` | Restore it from git history; if truly lost, that version can only be handled by a hand-written `Migration` or a destructive fallback |
| App upgrade lost all playlists / queue | Upgrading from a legacy version 1–6, which `fallbackToDestructiveMigrationFrom` wipes by design | Expected for pre-v7 installs only. If it happens upgrading from 7+, a migration is missing — investigate, don't accept it |
| Build error: "Not sure how to convert a Cursor to this method's return type" or "Type of the parameter must be a class annotated with @Entity" | DAO signature Room can't handle (e.g. returning a plain data class without `@Embedded`/`@Relation`, or non-suspend one-shot query) | Match existing DAO patterns: `suspend` for one-shots, `Flow<List<Entity>>` for reactive, POJOs with `@Embedded`+`@Relation` like `PlaylistWithSongs` |
| Build error: "Cannot figure out how to save this field into database" | New field type has no `@TypeConverter` | Add a pair of converters in `Converters.kt` (registered via `@TypeConverters(Converters::class)` on the database) |
| Runtime: "cannot find implementation for MusicDatabase. MusicDatabase_Impl does not exist" | KSP compiler entry missing for the target (usually `kspAndroid`) | Restore both `add("ksp...", "androidx.room:room-compiler:2.8.4")` lines |
| Flow query never emits updates | Writing through raw SQL to a different table than the one observed, or expecting `getPlaybackStateOnce()` (a `suspend`, not `Flow`) to be reactive | Room Flows only re-emit when an observed table changes; use a `Flow` query on the same table |
| `@Relation` query returns songs in random order | `PlaylistWithSongs.songs` is unordered by Room | Sort in the mapper — `PlaylistMapper.kt` already does `songs.sortedBy { it.playlistSong.position }`; keep that pattern |
| FK constraint failure inserting into `playlist_songs` | Song row not inserted first | Use `PlaylistDao.addSongToPlaylist(...)` (`@Transaction` that calls `insertSongIfMissing` first), don't insert `PlaylistSongEntity` directly |
