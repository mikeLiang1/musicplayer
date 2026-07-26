---
name: validate-changes
description: Use when you have made a code change and need to prove it is correct before calling it done — which tests to run, the fastest compile check, and per-area manual verification checklists.
---

# Validate Changes — the project's definition of done

All commands run from the repo root. PowerShell: `.\gradlew`; Git Bash: `./gradlew`.
Task names below were verified against `.\gradlew :composeApp:tasks --all` and the test task
was executed successfully (see "Expected output" below).

## 1. Fastest checks first

| Goal | Command | Notes |
|---|---|---|
| Type-check commonMain + androidMain without building an APK | `.\gradlew :composeApp:compileDebugKotlinAndroid` | Fastest full type-check; catches errors in both source sets because androidMain compiles against commonMain |
| Type-check commonMain only | `.\gradlew :composeApp:compileKotlinMetadata` | Slightly faster, but misses androidMain — prefer the one above |
| Run all unit tests | `.\gradlew :composeApp:testDebugUnitTest` | The project's unit test task (KMP module with only an Android target has no `jvmTest`; commonTest runs through the Android unit test variant) |
| Build an installable APK | `.\gradlew :composeApp:assembleDebug` | Only needed for manual verification; see `.claude/skills/build-and-run/SKILL.md` |

`testDebugUnitTest` also compiles everything the tests touch, so for most changes
`testDebugUnitTest` alone covers both the compile check and the tests.

### Expected output of a passing test run

Verified by actually running `.\gradlew :composeApp:testDebugUnitTest`:

- Gradle prints the task chain ending in `> Task :composeApp:testDebugUnitTest` and
  `BUILD SUCCESSFUL`. **Passing tests print nothing per-test** — silence plus
  BUILD SUCCESSFUL means green.
- Failures print the failing test names inline and the build fails; details land in
  `composeApp/build/reports/tests/testDebugUnitTest/index.html` and JUnit XML in
  `composeApp/build/test-results/testDebugUnitTest/`.
- Current baseline: 93 tests, 0 failures (QueueManagerTest 77, PlaybackRepositoryTest 15,
  ComposeAppCommonTest 1).
- Known noise in a clean run: a KMP/AGP deprecation warning at configure time, and about a
  dozen Kotlin warnings ("No cast needed", "Unnecessary non-null assertion") from
  `QueueManagerTest.kt` itself. These pre-exist; do not add new ones.

## 2. What the existing tests cover

All unit tests live in `composeApp/src/commonTest/kotlin/org/example/project/`:

**`core/manager/QueueManagerTest.kt`** (77 tests) — exercises the pure-Kotlin
`QueueManager` (`composeApp/src/commonMain/kotlin/org/example/project/core/manager/QueueManager.kt`)
directly, no fakes needed because it has no dependencies. Structure:

- Fixture `Song` objects (song1..song5) built inline; fresh `QueueManager` per test via
  `@BeforeTest`.
- Intent verification: `QueueManager` exposes `val _intent = Channel<QueueIntent>(Channel.UNLIMITED)`.
  Tests call a `drain()` helper that `tryReceive()`s everything into a list, then assert on
  the **last** intent's type and payload (e.g. `QueueIntent.SeekToItem(newIndex = 1)`,
  `ReplaceQueue`, `NewQueue`, `SeekAndRebuild`). Pattern for new tests: mutate → `drain()` →
  assert state via `manager.queueState.value` → assert last intent.
- Covered behaviors: `QueueState` computed properties (current/history/playbackQueue/
  playbackCurrentIndex/seenIds), setBaseQueue, playNext/playPrevious across base and manual
  queues, addToManualQueue (including uniqueId regeneration), playSongFromQueue (up-next,
  manual, history, unknown id), removeSong, shuffle/unshuffle including the
  preShuffleBaseQueue snapshot and radio-append reconciliation, togglePlaybackMode cycle
  (OFF → REPEAT → Infinite → OFF), hasNext/hasPrevious, replaceQueuesPreservingState,
  restoreState, plus full dual-queue lifecycle integration tests.

**`core/repository/PlaybackRepositoryTest.kt`** (15 tests) — exercises `PlaybackRepository`
against **`FakePlaybackDao`**, an in-memory fake defined at the bottom of the same test file.
It implements the `PlaybackDao` interface with a `MutableList<QueueEntity>` + a
`PlaybackStateEntity?` field, and counts `clearCount`/`insertCount` so tests can assert the
rewrite-skipping optimization (identical queue contents must NOT rewrite rows). The
repository is constructed with `Dispatchers.Unconfined` so its init-block seeding runs
synchronously. Covered: default-row seeding, position save/restore, full round-trip of
base/manual queues and song fields, shuffle-snapshot persistence regressions, seenIds
reconstruction on restore, legacy currentManualSongId fallback, and row-rewrite skipping.

**`ComposeAppCommonTest.kt`** — a single `assertEquals(3, 1 + 2)` template placeholder;
ignore it.

## 3. When to write new tests (project convention)

- **Do test:** anything in `core/manager/` and `core/repository/` that is pure Kotlin —
  `QueueManager`, `PlaybackRepository`, and by extension parsers (`core/parsers/`) if you
  change them. Follow the existing patterns: plain `kotlin.test`, `runBlocking` for suspend
  calls, hand-written fakes (no mocking library is on the classpath — only
  `libs.kotlin.test` is in `commonTest.dependencies`).
- **Not currently tested (convention, not aspiration):** ViewModels, Compose UI, and anything
  in `androidMain` (MediaService, MusicPlayerManagerImpl). These are verified manually.
  If you change them, the manual checklists below are the definition of done.
- A change to `QueueManager` or `PlaybackRepository` **without** a new/updated test is not
  done — the tests double as the regression suite for shuffle/restore bugs already fixed
  (see the "Shuffle snapshot (#3 regression)" section comment in PlaybackRepositoryTest).

## 4. Manual verification checklists (requires a connected device)

Build/install/launch and logcat tag details: `.claude/skills/build-and-run/SKILL.md`.

**Playback / queue changes** (deep dive: `.claude/skills/playback-and-queue/SKILL.md`)
1. Search for a song, tap it — song plays and a radio queue appears behind it.
2. Open the full player, swipe to the queue page; drag-reorder a song; verify order sticks
   and the playing song is unaffected.
3. Toggle shuffle on and off — order randomizes then restores (including songs radio
   appended while shuffled).
4. Cycle repeat: OFF → REPEAT → Infinite; verify end-of-queue behavior differs.
5. Add a song to the manual queue via the song menu ("play next" style), verify it plays
   before the remaining base queue.
6. Kill/restore persistence: play something, seek mid-song, swipe the app away from recents,
   relaunch — queue, current song, and position restore. Watch
   `adb logcat -s Logging:*` for "Task removed saved" / "OnDestroy saved".
7. If a song silently skips, check `adb logcat -s MediaService:*` for
   "Failed to resolve" / "Giving up resolving stream URL" (NewPipe URL resolution failure —
   possibly not your bug).

**Search changes** (API details: `.claude/skills/innertube-api/SKILL.md`)
1. Type a query slowly — debounced suggestions appear (NewPipe), then results on submit
   (InnerTube).
2. Scroll to the bottom of results — pagination loads more.
3. Voice search: tap the mic, grant RECORD_AUDIO on first use, speak — partial then final
   transcription fills the field. Denying the permission must not crash.
4. Network errors print via `println` in the repositories — watch
   `adb logcat -s System.out:*` (there is no Ktor logging plugin).

**Playlist / library changes** (schema details: `.claude/skills/room-database/SKILL.md`)
1. Create a playlist, add songs from the song menu, open it from the Library tab.
2. Remove a song, rename/delete the playlist — list state updates without restart.
3. Play from a playlist, then check Home tab: recently played row shows the song/playlist.
4. If you touched entities/DAOs: the DB is v8 and version bumps from 7 onward **must preserve
   data** via an `AutoMigration`. Verify by installing *over* the previous build (`adb install -r`,
   never uninstall first) and confirming playlists, liked songs and the restored queue survived —
   uninstalling hides exactly the bug you are checking for.

**Navigation changes** (structure: `.claude/skills/navigation/SKILL.md`)
1. Each bottom tab (Home / Search / Library) keeps its own back stack — navigate deep in
   Search, switch to Library and back — Search stack is preserved.
2. Playlist detail opens from both Search and Library (shared destination).
3. Mini player bar stays visible above bottom nav; tapping it opens the full-screen player
   overlay; back closes the overlay without losing the underlying tab position.
4. System back at a tab root behaves (no dead taps, no app-exit surprises mid-stack).

**UI component changes** — see `.claude/skills/ui-components/SKILL.md` for the SongItem
states and theming; verify each visual state you touched (playing/dragging/etc.) on device.

**New feature** — `.claude/skills/add-feature/SKILL.md` covers the scaffolding; validation
is then the union of the relevant checklists above.

## 5. "Before you claim done" checklist

1. `.\gradlew :composeApp:compileDebugKotlinAndroid` — compiles clean.
   You should see `BUILD SUCCESSFUL`; if it fails in a file you didn't touch, you likely
   changed a shared signature — fix all call sites, don't suppress.
2. `.\gradlew :composeApp:testDebugUnitTest` — all tests pass (silent + BUILD SUCCESSFUL).
   If you changed QueueManager/PlaybackRepository, you also **added or updated** a test.
3. No **new** Kotlin warnings in files you touched (the ~12 pre-existing QueueManagerTest
   warnings are the known baseline).
4. Device available → run the manual checklist for the affected area above. No device →
   say so explicitly in your summary ("compile + unit tests only; manual smoke not run —
   no device attached").
5. Release-relevant change (reflection, serialization, new dependency)? Also
   `.\gradlew :composeApp:assembleRelease` once — R8 is ON in release and can break what
   debug allows.

## Common failures

| Symptom | Cause | Fix |
|---|---|---|
| `Task 'jvmTest' not found` / `Task 'check' runs nothing useful` | Only target is Android; commonTest executes via the Android unit-test variant | Use `:composeApp:testDebugUnitTest` |
| Tests pass locally but intent assertions flaky | Forgot `drain()` before asserting, or asserting on first instead of last intent | Follow the drain-then-assert-last pattern in QueueManagerTest |
| New repository test hangs | Repository init seeding launched on a real dispatcher | Construct with `Dispatchers.Unconfined` like `PlaybackRepositoryTest.setup()` |
| "Cannot find implementation for ... MusicDatabase" when running the app after a DB change | KSP didn't regenerate | Re-run assemble; check both `kspCommonMainMetadata` and `kspAndroid` entries in `composeApp/build.gradle.kts` |
| Change works in debug, crashes in release | R8 minification (release only) | Keep rule in `composeApp/proguard-rules.pro`; verify with `assembleRelease` |
| Queue looks right in UI but wrong song plays | UI state (QueueManager) and player (MediaController) out of sync — an intent wasn't emitted/handled | Assert the emitted `QueueIntent` in a unit test; see `.claude/skills/playback-and-queue/SKILL.md` |
