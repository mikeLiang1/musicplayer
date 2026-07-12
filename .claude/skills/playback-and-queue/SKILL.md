---
name: playback-and-queue
description: Use when modifying the playback pipeline or queue engine, or debugging "song won't play / skips / wrong song plays next" issues — QueueManager, MusicPlayerViewModel, MediaService, stream URL resolution.
---

# Playback pipeline and queue engine

Three layers, strictly separated:

1. **`QueueManager`** (`core/manager/QueueManager.kt`, commonMain, pure Kotlin) — owns ALL
   queue logic: what is playing, what comes next. Emits `QueueIntent`s on a Channel.
2. **`MusicPlayerViewModel`** (`features/musicPlayer/ui/MusicPlayerViewModel.kt`) — the bridge.
   Collects `queueManager.intent` and translates each intent into `MusicPlayerManager` calls.
3. **`MusicPlayerManagerImpl`** (androidMain, `core/manager/MusicPlayerManagerImpl.kt`) — dumb
   `MediaController` wrapper. It never decides anything about the queue.
   The controller connects to **`MediaService`** (androidMain, `core/service/MediaService.kt`),
   a `MediaLibraryService` hosting the actual ExoPlayer.

Rule of thumb: queue decisions in QueueManager, playback mechanics in MediaService/ManagerImpl,
translation only in the ViewModel. Never shortcut a layer.

## End-to-end play flow (verified, numbered)

1. User taps a song (e.g. `SearchViewModel.OnSongClicked`, `HomeViewModel`) →
   `playSongUseCase(song.url)`.
2. `core/usecase/PlaySongUseCase.kt`: `innerTubeRepository.getRecommendations(songUrl)` fetches
   an InnerTube radio queue (~25 songs, tapped song first) → `queueManager.setBaseQueue(songs.songs)`.
   (Radio fetch details → `innertube-api` skill.) Note: the returned continuation token is
   dropped; `getMoreRecommendations`/`appendRadioSongs` exist but have **no production callers**.
3. `setBaseQueue` resets state (`autoPlay = true`, manual queue cleared, `seenIds` primed) and
   sends **`QueueIntent.NewQueue()`** — NOT `ReplaceQueue` — into `_intent`
   (`Channel(UNLIMITED)`).
4. `MusicPlayerViewModel.init` collects the intent. For `NewQueue` it calls
   `musicPlayerManager.setPlaylist(state.baseQueue, state.playbackCurrentIndex, positionMs, autoPlay)`.
   (Note the asymmetry: `NewQueue` passes `state.baseQueue`; all other intents operate on the
   flat `state.playbackQueue`. Equivalent right after `setBaseQueue` because the manual queue is
   empty, and after restore the flat list matters only once manual songs exist.)
5. `MusicPlayerManagerImpl.setPlaylist` → `MediaController.setMediaItems(mediaItems, startIndex, positionMs)`
   + `prepare()`. **The whole list is set on the controller at once**, one `MediaItem` per song
   (`core/helper/SongHelper.kt`: `mediaId = song.uniqueId`, `uri = song.url` — the plain
   YouTube **watch URL**, not a stream URL).
6. The controller talks to `MediaService`'s session. The session's player is
   `QueueForwardingPlayer` (wrapping ExoPlayer built with
   `DefaultMediaSourceFactory(ResolvingDataSource.Factory(DefaultHttpDataSource.Factory()))`).
7. **Stream URL resolution happens lazily inside the `ResolvingDataSource` lambda in
   `MediaService.onCreate`** — when ExoPlayer starts loading an item, the resolver receives the
   item's URI (the watch URL; the local variable is misleadingly named `youtubeId` — it is the
   full `https://www.youtube.com/watch?v=…` URL, which NewPipe accepts) →
   `resolveStreamUrl()`: check `urlCache` (expiry from the stream URL's `expire` query param
   minus a 5-min safety margin, else 1 h; max 100 entries) → `NewPipeRepository.getStreamUrl`
   (via `runBlocking(Dispatchers.IO)`, picks highest-bitrate audio stream) → retried **once** →
   `dataSpec.withUri(streamUrl)`. On double failure: `Log.e("MediaService", "Giving up …")`,
   fire-and-forget `queueManager.playNext()`, and throws `IOException` (skips the track).
8. ExoPlayer plays. There is no InnerTube involvement in playback; the `/player`-endpoint path
   (`InnerTubeRepository.getStreamUrl` + `PlayerParser`) is unused.

## Who advances the queue? (the #1 confusion)

**Both, in a fixed handshake:**

- **Natural song end → ExoPlayer advances itself.** It has the full playlist (step 5), so it
  simply transitions to the next media item. `MediaService`'s listener sees
  `onMediaItemTransition(reason == MEDIA_ITEM_TRANSITION_REASON_AUTO)` and calls
  `queueManager.playNext(fromAutoAdvanced = true)` — which updates QueueManager's pointer
  **without** emitting `SeekToItem` (no echo back to the player). Exception: if the ended song
  was a manual-queue song, the structure changed, so `playNext` emits `SeekAndRebuild` even for
  auto-advance.
- **User-driven next/previous → QueueManager decides, then commands the player.** UI buttons go
  `MusicPlayerViewModel.onNextClicked()` → `queueManager.playNext()`; notification/Bluetooth go
  through `QueueForwardingPlayer` (`core/service/QueueForwardingPlayer.kt`), which overrides
  `seekToNext/seekToPrevious/seekToNextMediaItem/seekToPreviousMediaItem` to call
  `queueManager.playNext()/playPrevious()` instead of letting ExoPlayer move, and overrides
  `hasNextMediaItem/hasPreviousMediaItem` with `queueManager.hasNext()/hasPrevious()` (this is
  what enables/greys the notification buttons).
- Player state flows back through `MusicPlayerManagerImpl`'s `Player.Listener` into
  `playerState: StateFlow<PlayerState>` (`isPlaying`, `isBuffering`, `durationMs`,
  `playWhenReady` — `core/model/PlayerState.kt`) and a 1-second polling loop for
  `currentPosition` (paused while app is backgrounded).

## QueueManager semantics (read `QueueManagerTest` before touching)

State (`QueueState` in `QueueManager.kt`): `baseQueue` (the radio/playlist),
`manualQueue` (user "play next" songs), `currentBaseIndex`, `currentManualSong`,
`isShuffled` + `preShuffleBaseQueue`/`preShuffleBaseIndex`, `playbackMode`, `seenIds`.

Derived (these define the flat list the player sees):
```
history            = baseQueue.take(currentBaseIndex [+1 if manual playing])
current            = currentManualSong ?: baseQueue[currentBaseIndex]
playbackQueue      = history + current + manualQueue + baseQueue.drop(currentBaseIndex + 1)
playbackCurrentIndex = history.size
```

- **Manual queue priority**: `playNext` plays `manualQueue[0]` before advancing the base queue.
  A playing manual song "pauses" the base pointer; the interrupted base song counts as history.
  `addToManualQueue` **copies the song with a fresh `uniqueId`** (same song may appear twice in
  the flat queue; `mediaId` must stay unique).
- **Shuffle** shuffles only `baseQueue[currentBaseIndex+1..end]` (history + current keep their
  positions), snapshots the full original into `preShuffleBaseQueue`; no-op if already shuffled.
  `unshuffle` restores the snapshot and re-finds the current song's index by `uniqueId`.
  `appendRadioSongs`/`removeSong`/`replaceQueuesPreservingState` all keep the snapshot in sync.
- **Repeat** (`PlaybackMode`: `OFF`, `REPEAT`, `Infinite`, cycled in that order) now has two
  effects: `hasNext()`/`hasPrevious()` (notification button enablement) AND real end-of-queue
  behavior via `MediaService`'s `Player.Listener.onPlaybackStateChanged(STATE_ENDED)` →
  `handleQueueEnded()`. This only fires at the *true* end of `baseQueue` with an empty manual
  queue — mid-queue advances still go through `onMediaItemTransition(REASON_AUTO)` as before.
  - `OFF` — no-op; playback stops at the end (unchanged, original behavior).
  - `REPEAT` — `queueManager.restartFromBeginning()`: `currentBaseIndex = 0`, emits
    `SeekToItem(0)`. Relies on the player's media-item list still matching `baseQueue` 1:1
    (true unless something desynced it) — no `setMediaItems` rebuild needed, just a seek.
  - `Infinite` — `MediaService.refillRadioQueue()`: fetches `innerTubeRepository
    .getRecommendations(lastSong.url)` (radio page for the last song), appends via
    `queueManager.appendRadioSongs(...)`, then `queueManager.playNext()` to advance into the
    first new song. Two intents (`ReplaceQueue` then `SeekToItem`) land on the same `Channel`
    in that order and are processed sequentially by `MusicPlayerViewModel`'s single collector —
    order is preserved because both `trySend` calls happen synchronously, back-to-back, from
    the same coroutine. **Known limitation**: `appendRadioSongs` dedupes against `seenIds`; if
    the API returns only already-seen songs, the append is a no-op and playback stays ended —
    not re-seeded with a different song. Network failure is caught and logged
    (`MediaService:E`, "Infinite radio refill failed for: ..."), playback just stays ended.
- `playSongFromQueue(uniqueId)` resolves where the id lives (current → no-op; history; manual —
  discards manual songs *before* it; upcoming base) and delegates to the private
  `selectHistory/Manual/NormalSong` helpers.
- `removeSong` only removes from `manualQueue` or the **upcoming** part of `baseQueue` (never
  current or history).

**Invariants to preserve when modifying** (all pinned by
`composeApp/src/commonTest/kotlin/org/example/project/core/manager/QueueManagerTest.kt`, 77 tests):
- `currentBaseIndex` stays within `baseQueue.indices` (clamp, never overflow).
- `currentManualSong` is never simultaneously inside `manualQueue`.
- `preShuffleBaseQueue` must contain the same song set as `baseQueue` (else unshuffle loses/resurrects songs).
- Every state mutation that changes the *flat list structure* must emit the right intent
  (see catalog below); index-only moves emit `SeekToItem`; **auto-advance emits nothing**.
- `seenIds` ⊇ ids in `baseQueue` (radio dedup).

Run the safety net: `./gradlew :composeApp:testDebugUnitTest` (commonTest executes on the
Android target — the only active target).

## QueueIntent catalog (`core/manager/QueueIntent.kt` → handled in `MusicPlayerViewModel.init`)

| Intent | Meaning | ViewModel translation |
|---|---|---|
| `NewQueue(positionMs)` | Brand-new queue (setBaseQueue / restore) | `setPlaylist(baseQueue, playbackCurrentIndex, positionMs, autoPlay)` — full `setMediaItems` |
| `ReplaceQueue(newIndex)` | List contents changed, current song unchanged (add-to-manual, remove, shuffle, drag-reorder) | `replaceFullQueueKeepingCurrentSong(playbackQueue, newIndex)` — surgical: removes/re-adds items **after** then **before** the controller's current item, never touching it (no audio gap) |
| `SeekToItem(newIndex)` | Only the playing position moved | `seekToDefaultPosition(newIndex)` |
| `SeekAndRebuild(mediaIndex, queueIndex)` | Position moved AND structure changed (manual-song transitions) | `seekToDefaultPosition(mediaIndex)` — an index into the **old** controller list — then `replaceFullQueueKeepingCurrentSong(playbackQueue, queueIndex)` |

The index math in `SeekAndRebuild` (old-list index vs new-list index) is the subtlest code in
the repo. If you change anything around manual-song selection, verify against the
`playSongFromQueue selects from manual queue` and `auto-advance through manual songs` tests.

## Persistence (`core/repository/PlaybackRepository.kt`)

- **Save**: `MusicPlayerViewModel.init` attaches (only *after* restore completes) a
  `queueState.debounce(500)` collector → `saveQueueState` — writes queue rows to Room only when
  contents actually changed (hash "signature"); pointer/shuffle/repeat updates are cheap.
  Playback **position** is saved separately by `MediaService.saveData()` in `onTaskRemoved` and
  `onDestroy` (`runBlocking` on purpose — the process may die immediately after).
- **Restore**: on app start, `MusicPlayerViewModel.init` → `restorePlaybackState()` →
  `getRestoredPlayback()` (rebuilds `QueueState` incl. shuffle snapshot and `seenIds`) →
  `queueManager.restoreState(state, positionMs)` → emits `NewQueue(positionMs)` → player is
  reloaded paused at the saved song/position (`autoPlay` is false in restored state).
- The intent collector is attached **before** restore runs; keep that ordering (the code
  comment explains why).

## Debugging playbook: "song won't play / skips / silent"

Real log tags (grep-verified):

```
adb logcat -s MediaService QueueForwardingPlayer System.out
```

- `MediaService` — stream-resolution failures: `"Failed to resolve: <url>"`, `"Giving up resolving stream URL for: <url>, skipping track"`.
- `System.out` — repository `println`s: `getStreamUrl error: …` (NewPipe), `searchSongs error: …`.
- `QueueForwardingPlayer` — logs every next/prev/hasNext call; tells you whether a skip came from notification/Bluetooth vs UI.
- `logging` (lowercase, `MusicPlayerManagerImpl`) — `"set playlist with position …"`, `"replacequeue running"`.
- `Logging` (capitalized, `MediaService`) — save-on-exit confirmations.

Decision tree:
1. **Track skips immediately, toast-less** → resolution failure. Look for `MediaService: Giving up…`. Causes: NewPipe extractor outdated vs YouTube (update `newpipe` deps in `gradle/libs.versions.toml`), age-restricted/region-blocked video, 429 → `ReCaptchaException` from `DownloaderImpl`.
2. **"Couldn't start playback" toast on tap** → the radio fetch failed, not playback: `PlaySongUseCase` → InnerTube `/next` (see `innertube-api` skill).
3. **Playback dies mid-song / after pause-resume (HTTP 403)** → cached stream URL expired. Check `parseExpiry` math in `MediaService`; the `expire` param and 5-min margin should prevent this — a 403 means the cache returned a stale URL or YouTube invalidated it early. Workaround: clear `urlCache` entry / restart playback.
4. **Wrong song plays next / UI queue disagrees with audio** → intent/state desync. Log `queueManager.queueState.value.playbackQueue` vs the controller's media items; check that the mutation you added emits the correct `QueueIntent` per the catalog above.
5. **Nothing plays, no logs at all** → controller not connected: `MusicPlayerManagerImpl.initialise()` runs in `MainActivity.onStart`; controller methods silently no-op while `controller == null` (all calls are `controller?.…`).

## Danger zones — do not change casually

- **`resolveStreamUrl` threading**: runs on ExoPlayer's internal loading thread; `runBlocking(Dispatchers.IO)` there is intentional. Never call it from the main thread, and don't make it suspend without rethinking `ResolvingDataSource`.
- **`QueueForwardingPlayer` overrides**: deleting any `seekTo*`/`has*MediaItem` override reintroduces raw ExoPlayer navigation that bypasses the manual queue and desyncs QueueManager.
- **`onMediaItemTransition` REASON_AUTO handler** in `MediaService` + `fromAutoAdvanced = true`: this pair prevents an echo loop (player advances → QueueManager syncs → must NOT re-seek the player). Breaking it causes double-skips.
- **`replaceFullQueueKeepingCurrentSong`** (`MusicPlayerManagerImpl`): the remove/add order (after-current first, then before-current) is what keeps indices valid mid-mutation. Reordering those four lines corrupts the controller list.
- **MediaController lifecycle**: built async in `initializeController`; `MainActivity.onStart` calls `initialise()` (reconnects if needed). Don't hold the controller past `release()`.
- **Restore-before-save ordering** in `MusicPlayerViewModel.init`: the debounced save is attached only after restore; attaching earlier persists the empty pre-restore state over good data (the comment in the code says exactly this).
- **`_intent` is a public `Channel` only so tests can drain it** (`QueueManagerTest.drain()`); don't add production consumers besides the ViewModel — a Channel delivers each element to a single consumer.
