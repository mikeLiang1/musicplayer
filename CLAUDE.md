# MusicPlayer — Kotlin Multiplatform

## What it is

A Kotlin Multiplatform (KMP) music player that streams audio from YouTube Music. It uses InnerTube (YouTube Music's internal API) for search, recommendations, and radio playlists, and NewPipe Extractor to resolve streaming URLs. The UI is built entirely with Compose Multiplatform and targets Android (iOS setup exists but the Kotlin iOS source set and build targets are commented out). Users can search for songs, view and manage playlists (via Room), play songs with radio-generated queues, reorder the queue, shuffle/repeat, and see recently played items.

## Module structure

The project is a single KMP module (`:composeApp`) under `composeApp/src/`:

- **`commonMain/`** (79 Kotlin files) — All shared code: data models, Room database + DAOs + entities, Koin DI modules, Ktor-based InnerTube repository, NewPipe repository, queue management engine (pure Kotlin), use cases, Compose UI (features/home, search, library, playlist, musicPlayer, songMenu, dashboard), shared UI components, theming, and navigation.
- **`androidMain/`** (9 Kotlin files) — Platform-specific: `MainActivity`, `MainApplication` (Koin init), `MusicPlayerManagerImpl` (ExoPlayer via `MediaController`), `MediaService` (Android `MediaLibraryService` that resolves stream URLs via NewPipe), an OkHttp-based `DownloaderImpl` for NewPipe, and the Room database builder.
- **`iosMain/`** (2 Kotlin files) — `MainViewController.kt` (wraps `App()` in `ComposeUIViewController`) and `Platform.ios.kt`. The iOS Kotlin targets are commented out in `build.gradle.kts`, so this is currently non-functional scaffolding.
- **`commonTest/`** — Shared tests (minimal).
- **`iosApp/`** — Standalone Xcode project wrapper (not wired to the KMP build).

## Architecture

**MVVM with a feature-per-package structure.** Each feature has its own `ui/` subdirectory containing a screen composable and its corresponding ViewModel. The ViewModels use `MutableStateFlow` for UI state and `MutableSharedFlow` for one-shot side effects (Effects pattern). ViewModels are AndroidX Lifecycle ViewModels (via JetBrains multiplatform lifecycle). The player system has a clean separation: `MusicPlayerManager` (interface in commonMain, platform impl in androidMain) handles raw play/pause/seek, while `QueueManager` (pure Kotlin in commonMain) owns queue logic (dual-queue with base + manual, shuffle, repeat modes, history). `MusicPlayerViewModel` bridges the two by translating UI actions into intents sent to both managers.

Data flow: **UI → Action → ViewModel → UseCase/Repository → DAO/API → Room/Ktor**.

## Key dependencies

| Category | Library |
|---|---|
| **DI** | Koin 4.2 (`koin-core`, `koin-compose`, `koin-compose-viewmodel`) |
| **Networking** | Ktor 3.4.0 (OkHttp engine, Content Negotiation, kotlinx-serialization-json) |
| **Database** | Room 2.8.4 (KSP, KMP), DataStore Preferences 1.2.0 |
| **Media playback** | AndroidX Media3 (ExoPlayer 1.9.1, Session, UI Compose) |
| **Image loading** | Coil 3.3.0 (`coil-compose`, `coil-network-ktor3`) |
| **Navigation** | AndroidX Navigation3 (`navigation3-runtime`, `navigation3-ui`, `lifecycle-viewmodel-navigation3`) |
| **YouTube** | NewPipe Extractor KMP 1.0, NewPipe Extractor 0.26.3 |
| **UI** | Compose Multiplatform 1.9.3, Material3, Material Icons Extended |
| **Reorderable lists** | `sh.calvin.reorderable:reorderable` 3.0.0 |

## Navigation

Uses **AndroidX Navigation3** (the JetBrains Compose Multiplatform navigation library). Routes are defined as `@Serializable` sealed interface members in `Route.kt`. A `Navigator` class handles `navigate`, `goBack`, `replaceRoot`, and per-tab back stacks via `NavigationState`. The top-level structure: `AppNavigation` → `DashboardNavigation` (the main scaffold with bottom nav: Home / Search / Library tabs). The playlist detail screen is a shared destination accessible from both Search and Library.

## Build commands

**Android:** `./gradlew :composeApp:assembleDebug` (Android target uses JVM 11)

**iOS:** The KMP iOS source set and build targets (`iosArm64`, `iosSimulatorArm64`) are **commented out** in `composeApp/build.gradle.kts`. There is an `iosApp/` Xcode project directory but it is not wired into the KMP build. Building for iOS would require uncommenting the iOS targets, adding the iOS framework export to the Xcode project, and implementing platform-specific code (at minimum a `MusicPlayerManager` for iOS).

## File map

### Core (data / domain layer)
| File | Role |
|---|---|
| `core/manager/QueueManager.kt` | Dual-queue engine: base + manual queues, shuffle, repeat, history, playNext/Previous |
| `core/manager/MusicPlayerManager.kt` | Interface contract for the platform-specific media player |
| `core/manager/QueueIntent.kt` | Sealed class defining queue mutation intents (ReplaceQueue, SeekToItem, etc.) |
| `core/repository/InnerTubeRepository.kt` | YouTube Music API via Ktor: search, recommendations, player endpoints |
| `core/repository/NewPipeRepository.kt` | Stream URL resolution + search suggestions via NewPipe Extractor |
| `core/repository/PlaybackRepository.kt` | Persist/restore queue state and playback position via Room |
| `core/repository/RecentlyPlayedRepository.kt` | Record recently played songs and playlists |
| `core/parsers/SearchParser.kt` | Manual JSON tree parsing for InnerTube search responses |
| `core/parsers/QueueParser.kt` | Manual JSON tree parsing for InnerTube radio queue responses |
| `core/database/MusicDatabase.kt` | Room database (v5, destructive migration) |
| `core/database/dao/*` | Room DAOs: PlaylistDao, PlaybackDao, RecentlyPlayedDao |
| `core/database/entity/*` | Room entities + mappers |
| `core/helper/InnerTubeExtensions.kt` | JSON traversal utilities for InnerTube responses |
| `core/helper/HttpHelpers.kt` | InnerTube request body builders |
| `core/helper/SongHelper.kt` | Song → MediaItem conversion |
| `core/usecase/PlaySongUseCase.kt` | Fetches radio queue for a song and sets it as the active queue |
| `core/di/AppModule.kt` | All Koin module definitions combined |

### Features (UI layer)
| File | Role |
|---|---|
| `features/musicPlayer/ui/MusicPlayerViewModel.kt` | Central orchestrator: bridges QueueManager ↔ MusicPlayerManager |
| `features/musicPlayer/ui/MusicPlayerScreen.kt` | Full-screen player with HorizontalPager (Now Playing + Queue) |
| `features/musicPlayer/ui/MusicPlayerBar.kt` | Mini-player bar shown above bottom navigation |
| `features/musicPlayer/ui/SongScreen.kt` | Now-playing song detail with seek bar and recommendations |
| `features/musicPlayer/ui/QueueScreen.kt` | Queue list with drag-reorder, history, repeat mode |
| `features/home/ui/HomeScreen.kt` | Home tab: greeting + recently played horizontal row |
| `features/home/ui/HomeViewModel.kt` | Observes recently played, dispatches play/navigation |
| `features/search/ui/SearchScreen.kt` | Suggestions + search results with pagination |
| `features/search/ui/SearchViewModel.kt` | Debounced search: NewPipe suggestions → InnerTube results |
| `features/library/ui/LibraryScreen.kt` | Library tab: filter tabs + playlist list |
| `features/library/ui/LibraryViewModel.kt` | Combines Room playlist data + filter state |
| `features/playlist/ui/PlaylistScreen.kt` | Playlist detail: songs with playback and editing |
| `features/playlist/repository/PlaylistRepository.kt` | Room-based playlist CRUD |
| `features/songMenu/ui/SongMenuViewModel.kt` | Song context menu (add to queue/playlist, remove) |
| `features/songMenu/ui/SongMenuController.kt` | Wires ViewModel + bottom sheets for song menu |
| `features/dashboard/navigation/DashboardNavigation.kt` | Scaffold with bottom nav + player bar + full-screen overlay |

### Navigation & shared UI
| File | Role |
|---|---|
| `navigation/Route.kt` | Type-safe `@Serializable` route definitions |
| `navigation/Navigator.kt` | Navigate, goBack, replaceRoot, per-tab back stacks |
| `navigation/AppNavigation.kt` | Top-level NavDisplay wiring |
| `ui/component/SongItem.kt` | Reusable song row (5 visual states, equalizer, drag handle) |
| `ui/component/MusicSearchBar.kt` | Search bar with debounced input and suggestions |
| `ui/component/CoverImage.kt` | Async image loader (Coil) |
| `ui/component/PlayPauseButton.kt` | Animated play/pause FAB |
| `ui/theme/Theme.kt` | BudgetTheme composable + color scheme |
| `App.kt` | Root composable: `BudgetTheme { AppNavigation() }` |

### Platform-specific
| File | Role |
|---|---|
| `androidMain/MainActivity.kt` | Android entry, EdgeToEdge, NewPipe init |
| `androidMain/MainApplication.kt` | Koin `startKoin` |
| `androidMain/core/manager/MusicPlayerManagerImpl.kt` | ExoPlayer via MediaController |
| `androidMain/core/service/MediaService.kt` | MediaLibraryService: stream URL resolution, ExoPlayer, notifications |
| `androidMain/core/di/AndroidModule.kt` | Platform Koin module (Room builder, ExoPlayer, DataStore) |
| `iosMain/MainViewController.kt` | `ComposeUIViewController { App() }` (non-functional — targets commented out) |

---

## AUDIT — for review, not yet acted on

### Largest files in commonMain (top 10 by line count)
1. `features/musicPlayer/ui/QueueScreen.kt` — 429 lines
2. `core/manager/QueueManager.kt` — 425 lines
3. `features/musicPlayer/ui/MusicPlayerScreen.kt` — 355 lines
4. `ui/component/SongItem.kt` — 286 lines
5. `features/musicPlayer/ui/SongScreen.kt` — 271 lines
6. `features/musicPlayer/ui/MusicPlayerViewModel.kt` — 242 lines
7. `features/playlist/ui/PlaylistScreen.kt` — 227 lines
8. `ui/component/MusicSearchBar.kt` — 213 lines
9. `features/library/ui/LibraryScreen.kt` — 209 lines
10. `features/search/ui/SearchViewModel.kt` — 160 lines

### Dead code / commented-out blocks
- **3 entire files** in `core/model/network/` are 100% commented out: `SearchResponse.kt` (31 lines), `MusicResponsiveListItemRenderer.kt` (114 lines), `Tabs.kt` (26 lines). These were typed Kotlin serialization models replaced by manual `JsonObject` tree parsing in `InnerTubeExtensions.kt`.
- **`Continuation.kt`** (core/model/network/) — the data class and its `getContinuation()` extension are never imported or called.
- **`QueueScreen.kt` lines 261-324** — a 64-line `CurrentSongRow` composable is fully commented out.
- **`InnerTubeRepository.kt` lines 95-123** — a 29-line `getStreamUrl()` method is commented out (replaced by NewPipe resolution in `MediaService`).
- **`AppNavigation.kt` lines 27-33** — commented-out login route entry (7 lines).
- **`Type.kt` lines 32-47** — 16-line commented duplicate typography block (template remnant).
- **`Song.kt`** — `mockSongList` is public but never imported/used anywhere.
- **`SearchParser.kt:116-125`** — `parseByline()` extension function defined but never called.
- **`InnerTubeRepository.kt:125-148`** — `buildPlayerBody()` and `decodeSignatureCipher()` defined but never called.
- **4 spurious `import org.schabi.newpipe.extractor.timeago.patterns.it`** imports: in `MusicPlayerManager.kt`, `InnerTubeRepository.kt`, `SearchParser.kt` (these import the `it` extension but never use it; leftover from NewPipe time-ago parsing).
- **1 spurious `import org.slf4j.MDC.put`** in `InnerTubeRepository.kt`.

### Package name inconsistency (now fixed)
- `NavigationState.kt` used `package com.example.budget.navigation` — **fixed**, now `org.example.project.navigation`.
- `Type.kt`, `Dimens.kt`, `Shape.kt` used `package com.example.budget.ui.theme` — **fixed**, now `org.example.project.ui.theme`.
