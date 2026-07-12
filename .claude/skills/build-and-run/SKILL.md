---
name: build-and-run
description: Use when you need to build, install, launch, or watch logs for the MusicPlayer Android app (debug or release APK, adb, logcat filtering, common Gradle build failures).
---

# Build and Run

Single KMP module `:composeApp`, Android-only target (iOS targets are commented out in
`composeApp/build.gradle.kts`). All commands run from the repo root.

Shell note: PowerShell uses `.\gradlew`, Git Bash uses `./gradlew`. Everything else on the
command line is identical in both shells. All Gradle task names below were verified against
`.\gradlew :composeApp:tasks --all` output.

## Key identifiers (verified in `composeApp/build.gradle.kts` and `composeApp/src/androidMain/AndroidManifest.xml`)

| Thing | Value |
|---|---|
| applicationId (release) | `org.example.project` |
| applicationId (debug) | `org.example.project.debug` (debug adds `applicationIdSuffix = ".debug"`) |
| Launcher activity | `org.example.project.MainActivity` |
| App label | "KotlinProject" (release) / "KotlinProject Debug" (debug) — set via manifest placeholders |
| Debug APK output | `composeApp/build/outputs/apk/debug/composeApp-debug.apk` |
| Release APK output | `composeApp/build/outputs/apk/release/composeApp-release.apk` |

Because of the `.debug` suffix, debug and release install **side by side** as two separate
apps with different names and icons (`ic_launcher_debug` vs `ic_launcher`).

## Recipe: debug build → install → launch → logs

1. Build the debug APK:
   ```
   .\gradlew :composeApp:assembleDebug
   ```
   Checkpoint: ends with `BUILD SUCCESSFUL` and the APK exists at
   `composeApp/build/outputs/apk/debug/composeApp-debug.apk`. A warning about
   `org.jetbrains.kotlin.multiplatform` + `com.android.application` compatibility with AGP 9.0
   may be printed at configure time (it is skipped when the Gradle configuration cache is
   reused) — this is expected noise, not a failure.

2. Install to the connected device/emulator (requires a connected device):
   ```
   .\gradlew :composeApp:installDebug
   ```
   or, from an already-built APK:
   ```
   adb install -r composeApp/build/outputs/apk/debug/composeApp-debug.apk
   ```
   Checkpoint: `adb shell pm list packages | grep org.example.project` (PowerShell:
   `adb shell pm list packages | Select-String org.example.project`) shows
   `package:org.example.project.debug`. If `adb` reports "no devices/emulators found",
   start an emulator or plug in a device with USB debugging and check `adb devices`.

3. Launch the debug app:
   ```
   adb shell am start -n org.example.project.debug/org.example.project.MainActivity
   ```
   Note the component syntax: the part before `/` is the *runtime* package
   (`org.example.project.debug`), the part after is the *class* name, which keeps its
   original package (`org.example.project.MainActivity`). For the release build use
   `org.example.project/org.example.project.MainActivity`.

4. Watch logs (see "Logcat filtering" below).

## Release build and the keystore.properties fallback

`composeApp/build.gradle.kts` (lines ~103-168) loads signing credentials from a
**git-ignored** `keystore.properties` at the repo root. The logic:

- If `keystore.properties` exists → the `release` signing config is populated from its
  `storeFile` / `storePassword` / `keyAlias` / `keyPassword` properties and used.
- If it does **not** exist → the release build type falls back to
  `signingConfigs.getByName("debug")`, so `assembleRelease` still produces an installable
  (debug-signed) APK. This is intentional so anyone can build and test release locally.

Release also has `isMinifyEnabled = true` and `isShrinkResources = true` with
`composeApp/proguard-rules.pro` applied — so release builds are slower, and R8 can break
things at runtime that work fine in debug (reflection, serialization). If a bug only
reproduces in release, suspect R8/ProGuard first.

Commands (both verified to exist):
```
.\gradlew :composeApp:assembleRelease
.\gradlew :composeApp:installRelease
```
APK lands at `composeApp/build/outputs/apk/release/composeApp-release.apk`.

Caution: if you later add a real `keystore.properties`, a device that already has the
debug-signed release app installed will refuse the properly-signed one with
`INSTALL_FAILED_UPDATE_INCOMPATIBLE` — uninstall first (`.\gradlew :composeApp:uninstallRelease`
or `adb uninstall org.example.project`).

## Logcat filtering for this app

The codebase uses only a handful of real `Log` tags (all in `androidMain`); shared code in
`commonMain` cannot use `android.util.Log`, so repositories print with `println`, which
surfaces in logcat under the `System.out` tag.

| Tag | Where | What you see |
|---|---|---|
| `MediaService` | `composeApp/src/androidMain/kotlin/org/example/project/core/service/MediaService.kt` | `Log.e` on stream-URL resolution failures ("Failed to resolve: …", "Giving up resolving stream URL for: …") |
| `Logging` (capital L) | `MediaService.kt` | `Log.d` when playback state is saved on task removal / service destroy ("Task removed saved", "OnDestroy saved") |
| `logging` (lowercase) | `composeApp/src/androidMain/kotlin/org/example/project/core/manager/MusicPlayerManagerImpl.kt` | `Log.d` from the MediaController side |
| `QueueForwardingPlayer` | `composeApp/src/androidMain/kotlin/org/example/project/core/service/QueueForwardingPlayer.kt` | `Log.d` for next/previous/seek forwarding |
| `System.out` | `println` in `commonMain` repositories (`InnerTubeRepository.kt`, `NewPipeRepository.kt`) | Network/parsing error messages — there is no Ktor logging plugin, these printlns are the only network error trace |

Useful invocations (identical in both shells; requires a connected device):
```
adb logcat -s MediaService:* Logging:* logging:* QueueForwardingPlayer:*    # app's own tags
adb logcat -s System.out:*                                                  # commonMain println output
adb logcat --pid=$(adb shell pidof -s org.example.project.debug)            # everything from the app
```
The `--pid=$(...)` form works in **both** shells — PowerShell subexpressions also use `$(...)`.
Do not drop the `$` in PowerShell: a bare `--pid=(...)` splits into two separate arguments
(`--pid=` plus the value) and logcat rejects the empty `--pid=`.
Note `pidof` only works while the app process is running. For crashes, add
`AndroidRuntime:E` to the `-s` list or just run `adb logcat *:E`.

## Verified Gradle task inventory

Verified present via `.\gradlew :composeApp:tasks --all` (2026-07):

- `assembleDebug`, `assembleRelease`, `assemble`
- `installDebug`, `installRelease`, `uninstallDebug`, `uninstallRelease`, `uninstallAll`
- `testDebugUnitTest`, `testReleaseUnitTest`, `test`
- `compileDebugKotlinAndroid` (fast type-check without packaging an APK)
- `compileKotlinMetadata` (compiles commonMain metadata only)
- `bundleDebug` / `bundleRelease` (AABs, rarely needed)

There are no flavor variants — just `debug` and `release`.

## Room/KSP build wiring (why builds fail here)

- Room's compiler is registered twice in `composeApp/build.gradle.kts` `dependencies`:
  `add("kspCommonMainMetadata", "androidx.room:room-compiler:2.8.4")` and
  `add("kspAndroid", ...)`. Both are required for a KMP module: the metadata pass processes
  `commonMain` and the android pass generates the actual implementation. If you see
  "Cannot find implementation for org.example.project.core.database.MusicDatabase" at runtime,
  the `kspAndroid` entry is missing or KSP didn't run.
- `room { schemaDirectory("$projectDir/schemas") }` (must sit **outside** the `android {}`
  block) makes Room export a JSON schema per DB version into `composeApp/schemas/` — the
  `copyRoomSchemas` task copies the KSP-generated schemas there. Versions 1-5 live under
  `composeApp/schemas/org.example.project.core.database.MusicDatabase/`; the sibling
  `org.example.project.core.dao.MusicDatabase/1.json` is a stale leftover from before the
  class moved packages — ignore it. Schemas should be committed; the DB is currently v5 with
  destructive migration, so bumping the version writes a new JSON and wipes user data on
  upgrade.
- JVM target is pinned to 11 in two places (Kotlin `jvmTarget.set(JvmTarget.JVM_11)` and
  `compileOptions` source/target compatibility). Build with a JDK 11+ (17 works); mismatched
  local JDK settings produce "Inconsistent JVM-target compatibility" errors.

## Common failures

| Symptom | Cause | Fix |
|---|---|---|
| `INSTALL_FAILED_UPDATE_INCOMPATIBLE` on install | Same applicationId previously installed with a different signing key (e.g. debug-signed release vs keystore-signed release) | `adb uninstall org.example.project` (or `.debug`) then reinstall |
| App installs but "no devices/emulators found" earlier | No device attached / emulator not booted | `adb devices`; start emulator; enable USB debugging |
| Runtime crash: "Cannot find implementation for ... MusicDatabase" | KSP Room compiler entry missing/not run | Ensure both `kspCommonMainMetadata` and `kspAndroid` room-compiler lines exist in `composeApp/build.gradle.kts`; re-run a clean assemble |
| `Inconsistent JVM-target compatibility` / "Unknown Kotlin JVM target" | Local JDK or IDE Gradle JVM mismatched with the pinned JVM 11 target | Use JDK 11+; don't edit the jvmTarget in build.gradle.kts |
| Build warns about `org.jetbrains.kotlin.multiplatform` vs `com.android.application` AGP 9.0 | Known deprecation in this project structure | Expected noise on every build today; ignore unless upgrading AGP to 9.x |
| `Unresolved reference` only for iOS symbols / iosMain | iOS targets are commented out; `iosMain` is unwired scaffolding | Don't try to compile iosMain; see build.gradle.kts lines 27-35 |
| Release-only runtime crash that debug doesn't have | R8 minification (`isMinifyEnabled = true`) stripped/renamed something | Add a keep rule in `composeApp/proguard-rules.pro`; retest `assembleRelease` |
| Launch intent fails: "Activity class does not exist" | Used `.debug` suffix on the class part of the component | Class stays `org.example.project.MainActivity`; only the package before `/` gets `.debug` |

## Related

- To validate a change (tests, compile-check, manual smoke checklist): see
  `.claude/skills/validate-changes/SKILL.md`.
- Playback internals and queue debugging: `.claude/skills/playback-and-queue/SKILL.md`.
