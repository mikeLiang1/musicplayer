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

## Current schema (version 5)

Entities registered in `MusicDatabase.kt`:

```kotlin
@Database(
    entities = [
        QueueEntity::class, PlaybackStateEntity::class, PlaylistEntity::class,
        PlaylistSongEntity::class, SongEntity::class, RecentlyPlayedEntity::class
    ],
    version = 5
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

Destructive migration is configured in **commonMain**, in `getRoomDatabase()` at the bottom of `MusicDatabase.kt` (not in the Android builder — `getDatabase.kt` only supplies the file path `music.db`):

```kotlin
return builder
    .setDriver(BundledSQLiteDriver()) // Use the bundled driver for KMP
    .setQueryCoroutineContext(Dispatchers.IO)
    .fallbackToDestructiveMigration(true)
    .build()
```

**Consequence: any schema change + version bump WIPES ALL USER DATA on app upgrade** — every playlist, every song saved to a playlist, recently played history, and the persisted queue/playback position. There are no `Migration` objects anywhere in the repo.

- This is the accepted trade-off today. If you change schema, you MUST bump `version` in the `@Database` annotation, and you must accept (and tell the user) that upgrading wipes the DB.
- If the team ever wants to preserve data: write a `Migration(from, to)` and pass it via `builder.addMigrations(...)` in `getRoomDatabase()`, and remove `fallbackToDestructiveMigration(true)`. The historical schema JSONs in `composeApp/schemas/` exist precisely so you can diff versions when writing that migration.

## Recipe: add a column (or a new entity) end-to-end

1. **Entity** — edit/create the data class in `core/database/entity/`. For a new column give it a default value in Kotlin (Room still treats it as a schema change). For a new entity, set `tableName` explicitly (the older entities that didn't are stuck with class-name tables).
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
7. **Bump version** — `version = 5` → `6` in the `@Database` annotation. Skipping this on an already-installed app crashes with an integrity error (see failures table).
8. **Rebuild** — `./gradlew :composeApp:assembleDebug`. KSP regenerates the DAO/database impls and writes `composeApp/schemas/org.example.project.core.database.MusicDatabase/6.json`. Commit that JSON.
9. **Verify** — install, exercise the feature, and remember the DB was just wiped, so re-create test playlists.

## KSP / Gradle gotchas

- BOTH compiler entries in `composeApp/build.gradle.kts` are required; if you touch them keep both:
  ```kotlin
  add("kspCommonMainMetadata", "androidx.room:room-compiler:2.8.4")
  add("kspAndroid", "androidx.room:room-compiler:2.8.4")
  ```
  Missing `kspAndroid` → "cannot find implementation for MusicDatabase" at runtime. Missing `kspCommonMainMetadata` → unresolved generated symbols when compiling common metadata.
- `room { schemaDirectory("$projectDir/schemas") }` sits OUTSIDE the `android {}` block (comment in the file says MUST).
- `composeApp/schemas/` currently contains versions 1–5 under `org.example.project.core.database.MusicDatabase/`, plus a stale leftover directory `org.example.project.core.dao.MusicDatabase/1.json` from before the class moved packages — ignore it, don't "fix" it.
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
| App upgrade silently lost all playlists / queue | That's `fallbackToDestructiveMigration(true)` doing its job after a version bump | Expected. To preserve data, write a `Migration` (see policy section) |
| Build error: "Not sure how to convert a Cursor to this method's return type" or "Type of the parameter must be a class annotated with @Entity" | DAO signature Room can't handle (e.g. returning a plain data class without `@Embedded`/`@Relation`, or non-suspend one-shot query) | Match existing DAO patterns: `suspend` for one-shots, `Flow<List<Entity>>` for reactive, POJOs with `@Embedded`+`@Relation` like `PlaylistWithSongs` |
| Build error: "Cannot figure out how to save this field into database" | New field type has no `@TypeConverter` | Add a pair of converters in `Converters.kt` (registered via `@TypeConverters(Converters::class)` on the database) |
| Runtime: "cannot find implementation for MusicDatabase. MusicDatabase_Impl does not exist" | KSP compiler entry missing for the target (usually `kspAndroid`) | Restore both `add("ksp...", "androidx.room:room-compiler:2.8.4")` lines |
| Flow query never emits updates | Writing through raw SQL to a different table than the one observed, or expecting `getPlaybackStateOnce()` (a `suspend`, not `Flow`) to be reactive | Room Flows only re-emit when an observed table changes; use a `Flow` query on the same table |
| `@Relation` query returns songs in random order | `PlaylistWithSongs.songs` is unordered by Room | Sort in the mapper — `PlaylistMapper.kt` already does `songs.sortedBy { it.playlistSong.position }`; keep that pattern |
| FK constraint failure inserting into `playlist_songs` | Song row not inserted first | Use `PlaylistDao.addSongToPlaylist(...)` (`@Transaction` that calls `insertSongIfMissing` first), don't insert `PlaylistSongEntity` directly |
