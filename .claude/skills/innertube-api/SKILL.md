---
name: innertube-api
description: Use when debugging or extending the InnerTube (YouTube Music internal API) integration — empty search results, broken radio queues, parser crashes after YouTube changes response shapes, or adding new InnerTube endpoints.
---

# InnerTube API Integration

This app talks to YouTube Music's **internal** API ("InnerTube"). YouTube changes response
shapes silently and without notice; when search or radio "suddenly breaks", it is almost
always a parser/response-shape mismatch, not a code regression. This skill tells you how the
integration works and how to diagnose and fix it.

## Architecture: who calls what

All InnerTube calls live in `composeApp/src/commonMain/kotlin/org/example/project/core/repository/InnerTubeRepository.kt`.
Real endpoint constants (companion object of that file):

| Endpoint | Repository function | Body builder (`core/helper/HttpHelpers.kt`) | Parser |
|---|---|---|---|
| `https://music.youtube.com/youtubei/v1/next` | `getRecommendations(url)` — radio queue for a song | `buildNextBody(videoId)` | `parseQueuePage` |
| `https://music.youtube.com/youtubei/v1/next` | `getMoreRecommendations(token)` — **no production callers** (only the parser path exists) | `buildContinuationBody(token)` | `parseQueuePage` |
| `https://music.youtube.com/youtubei/v1/search` | `searchSongs(query)` | `buildSearchBody(query)` | `parseSearchPage` |
| `https://music.youtube.com/youtubei/v1/search` | `searchMoreSongs(token)` — pagination | `buildContinuationBody(token)` | `parseSearchPage` |
| `https://music.youtube.com/youtubei/v1/player` | `getStreamUrl(videoId)` — **not used in production**; stream URLs come from NewPipe (see below) | `buildPlayerBody(videoId)` | `parsePlayerResponse` |

There is **no API key**. Requests are plain POSTs; identity is asserted via the request body
`context.client` and headers.

### Client identity spoofed

From `HttpHelpers.kt` — the app pretends to be the YouTube Music **web** client:

- Body: `clientName: "WEB_REMIX"`, `clientVersion: CLIENT_VERSION`, `hl: "en"`, `gl: "US"`, `platform: "DESKTOP"` (next/player bodies)
- Headers (`applyHeaders()`): `X-YouTube-Client-Name: 67`, `X-YouTube-Client-Version`, desktop Chrome User-Agent, `Referer`/`Origin: https://music.youtube.com`
- `CLIENT_VERSION = "1.20241015.01.00"` lives in `InnerTubeRepository`'s companion object, with this comment: *"Update when YouTube starts rejecting requests (check DevTools → Network → clientVersion)"*. That is the first knob to turn when everything 4xx's at once.

Magic params in bodies:
- `buildNextBody`: `playlistId = "RDAMVM$videoId"`, `params = "wAEB"` → returns a full radio queue for the video.
- `buildSearchBody`: `params = "EgWKAQIIAWoKEAkQBRAKEAMQBA%3D%3D"` → "songs only" search filter. Because of this filter, the Albums/Artists shelves in `parseSearchPage` rarely appear in practice.

The Ktor `HttpClient` is built in `core/di/AppModule.kt` (`networkModule`): OkHttp engine,
`expectSuccess = true` (non-2xx throws), `ignoreUnknownKeys = true`, and a `defaultRequest`
base URL of `https://music.youtube.com/youtubei/v1/`. There is **no Ktor Logging plugin installed**.

## InnerTube vs NewPipe — division of labor

Verified from code:

- **InnerTube** (`InnerTubeRepository`): search results (`searchSongs`/`searchMoreSongs`), radio queue / recommendations (`getRecommendations`).
- **NewPipe** (`core/repository/NewPipeRepository.kt`): **stream URL resolution** (`getStreamUrl` — called only from `androidMain/.../core/service/MediaService.kt`'s `ResolvingDataSource`) and **search suggestions** (`getSearchSuggestions`, used by `SearchViewModel`'s debounced suggestion flow).
- NewPipe's HTTP layer is `androidMain/kotlin/org/example/project/DownloaderImpl.kt` (OkHttp): Firefox UA, a `SOCS` consent cookie for youtube.com URLs, and it maps HTTP 429 → `ReCaptchaException`. Initialized via `NewPipe.init(DownloaderImpl.init(...))` in `MainActivity.onCreate` (inside a `LaunchedEffect`; the UI does not render until this completes).
- `InnerTubeRepository.getStreamUrl` + `core/parsers/PlayerParser.kt` are a working but **unused** alternate path (Metrolist-style); playback does not go through them. Stream 403/expiry issues → see the `playback-and-queue` skill.

## Parsing philosophy — DO NOT resurrect typed models

Parsing is **manual `JsonObject` tree-walking**, on purpose. Responses are decoded to
`kotlinx.serialization.json.JsonObject` and walked with the helpers in
`core/helper/InnerTubeExtensions.kt`. The typed `@Serializable` models in
`core/model/network/` (`SearchResponse.kt`, `MusicResponsiveListItemRenderer.kt`, `Tabs.kt`)
are 100% commented-out dead code — they were abandoned precisely because YouTube's nesting
depth changes too often. Do not un-comment them; fix the tree-walkers instead.

### Helper contracts (`InnerTubeExtensions.kt`)

| Helper | Contract |
|---|---|
| `JsonElement.findObjectWithKey(key)` | Recursive depth-first search (objects + arrays) for the **first** object containing `key`; returns `this[key]?.jsonObject`. **The value at `key` must itself be a JSON object** — `.jsonObject` on a primitive throws `IllegalArgumentException`. (Note: `parseAlbumItem`/`parseArtistItem` call it on `browseId`, a string primitive — that path would throw if an Albums/Artists shelf ever appeared; the songs-only search filter is why it hasn't bitten. Caught by `searchSongs`'s try/catch → silently empty result.) |
| `JsonObject?.getRunsText()` | Joins all `runs[].text` into one string; returns `"Unknown"` if runs are missing. |
| `JsonObject?.getRuns()` | Specific to search rows: unwraps `musicResponsiveListItemFlexColumnRenderer.text.runs` → `List<JsonObject>?`. |
| `List<JsonObject>.chunkedBySeparator()` | Splits a runs list into groups on the `" • "` separator run (YouTube's "Artist • Album • 3:45" byline format). Always returns at least one (possibly empty) group. |
| `String.parseTimeToMillis()` | `"3:45"` / `"1:02:30"` → millis; anything else → `0L`. |
| `extractVideoId(url)` | Handles `v=`, `youtu.be/`, or a raw 11-char ID. Returns null otherwise. |

Because helpers are null-tolerant, shape changes usually manifest as **silently empty results**
rather than crashes.

## THE KEY PLAYBOOK — "search / radio suddenly returns empty or crashes"

### Step 1 — Capture the raw response

There is **no existing response-body logging**: no Ktor `Logging` plugin in `networkModule`,
and `DownloaderImpl.init` creates an `HttpLoggingInterceptor` but the `addInterceptor` line is
**commented out** (line 33) — and that only covers NewPipe traffic anyway. Errors go through
`println(...)` in the repository catch blocks, which appear in logcat under tag `System.out`.
(Exception: `getRecommendations` has **no** try/catch — its errors propagate to
`PlaySongUseCase`'s `runCatching` and surface as the "Couldn't start playback" toast, with no println.)

To capture an InnerTube response, add a temporary dump in the failing repository function,
e.g. in `searchSongs`:

```kotlin
import io.ktor.client.statement.bodyAsText
// ...
val raw = client.post(SEARCH_URL) { /* same as before */ }.bodyAsText()
println("INNERTUBE_RAW: ${raw.take(4000)}")   // logcat tag: System.out
val response: JsonObject = Json { ignoreUnknownKeys = true }.parseToJsonElement(raw).jsonObject
```

Filter logcat: `adb logcat -s System.out`. Responses are large — dump in chunks or write to a
file if 4000 chars isn't enough. **Remove the dump before committing.**

You can also reproduce outside the app with curl: POST the exact body from
`HttpHelpers.kt` with the headers from `applyHeaders()` to the endpoint URL.

### Step 2 — Diff the JSON tree against what the parser expects

Full key-path chains are in [reference.md](reference.md). Summary of what each parser walks:

- `parseSearchPage` (`core/parsers/SearchParser.kt`) — **first** checks for the continuation
  shape `…continuationContents.musicShelfContinuation.contents[]` (a "load more" response);
  otherwise: `findObjectWithKey("sectionListRenderer")` → `contents[]` →
  `musicShelfRenderer` → shelf `title.runs[0].text` must be exactly `"Songs"` / `"Albums"` /
  `"Artists"` → `contents[]` items.
- `parseSongItem` — `musicResponsiveListItemRenderer` → videoId from
  `findObjectWithKey("watchEndpoint").videoId`; title from `flexColumns[0]`; artist/album/
  duration from `flexColumns[1]` runs chunked on `" • "`; thumbnail from
  `thumbnail.musicThumbnailRenderer.thumbnail.thumbnails[last].url`.
- `parseQueuePage` (`core/parsers/QueueParser.kt`) — `findObjectWithKey("playlistPanelRenderer")`
  → `contents[]` → `playlistPanelVideoRenderer` per item (videoId, `title` runs,
  `longBylineText` runs split on `" • "`, `thumbnail.thumbnails[last].url`,
  `lengthText.runs[0].text`).

Typical YouTube changes: a renderer gets renamed (`musicShelfRenderer` →
`musicCardShelfRenderer`), a wrapper level is added/removed (usually absorbed by
`findObjectWithKey`), the shelf **title text changes** (localization — `hl: "en"` in the body
is what keeps titles English; if titles stop matching, check that first), or a field moves
inside the item renderer.

### Step 3 — Fix and verify

1. Adjust the key-path in the parser (prefer `findObjectWithKey` over hardcoding a deep chain
   when the level that changed is a wrapper).
2. Note: `SearchParser.kt` currently carries an unused `import org.schabi.newpipe.extractor.timeago.patterns.it`
   (also in `MusicPlayerManager.kt`) — harmless leftover; don't cargo-cult it into new files.
3. Verify checkpoints:
   - Build: `./gradlew :composeApp:assembleDebug`
   - Run, search for a common song → results appear with title/artist/thumbnail/duration.
   - Scroll to the bottom of results → more results load (continuation shape works too).
   - Tap a song → a radio queue of ~25 songs populates the queue screen (`parseQueuePage`).
   - Remove your temporary `println` dumps.

## Continuation / pagination (search)

Trace, verified in code:
1. Initial search: `SearchViewModel.handleAction(OnSuggestionClicked)` → `innerTubeRepository.searchSongs(query)` → `SearchResult.continuationToken` stored in `SearchUiState.searchToken`. The token is parsed in `SearchParser.parseContinuationToken()`: `musicShelfRenderer.continuations[0].nextContinuationData.continuation`.
2. Scroll triggers `SearchAction.SearchMoreSongs` → guarded by `isLoadingMore`/`onSearchScreen`/`isLoading`/null-token → `searchMoreSongs(token)` posts `buildContinuationBody(token)` to `/search`.
3. The continuation **response has a different shape** — songs live under `musicShelfContinuation`, with no `sectionListRenderer` and no shelf title. `parseSearchPage` handles this branch first; if that branch ever breaks, symptom = first page loads, pagination silently stops.
4. New songs appended to `songList`; new token replaces the old (null token ends pagination).

Radio-queue continuations (`nextRadioContinuationData.continuation`) are parsed into
`SongPage.continuationToken`, but `getMoreRecommendations` has no production caller — the
token is currently dropped by `PlaySongUseCase`.

## Common failures

| Symptom | Likely cause | Fix |
|---|---|---|
| All searches return empty, `searchSongs error:` in logcat (`System.out`) | HTTP-level rejection (`expectSuccess = true` throws on 4xx). YouTube rejecting the client version. | Bump `CLIENT_VERSION` in `InnerTubeRepository` (get current from music.youtube.com DevTools → any `youtubei` request → body `clientVersion`). |
| Searches return empty, **no** error printed | Response shape changed; parser walks to a null and returns `SearchResult()` | Playbook above — dump raw JSON, diff against reference.md paths. |
| First page works, "load more" does nothing | `musicShelfContinuation` branch or `nextContinuationData` token path changed | Check both the token extraction and the continuation-response branch in `parseSearchPage`. |
| Tap a song → "Couldn't start playback" toast | `/next` request threw (`getRecommendations` has no try/catch; the exception reaches `PlaySongUseCase`'s `runCatching`) | Likely connectivity or client-version rejection — bump `CLIENT_VERSION`; dump the `getRecommendations` response. |
| Tap a song → nothing plays, **no** toast | `parseQueuePage` mismatch (`playlistPanelRenderer` / `playlistPanelVideoRenderer`) — an empty queue is set silently (`MusicPlayerViewModel` ignores intents when the queue is empty) | Dump `getRecommendations` response; verify `params: "wAEB"` and `RDAMVM` playlistId still return a radio panel. |
| Missing thumbnails | `thumbnail.musicThumbnailRenderer.thumbnail.thumbnails` (search) or `thumbnail.thumbnails` (queue) moved | Fix path in `parseSongItem` / `parseQueueItem`; parsers take `lastOrNull()` = largest image. |
| Wrong/zero durations | Byline chunking: duration is assumed to be the **last** `" • "`-group (search) or `lengthText.runs[0]` (queue) | Inspect the actual runs; adjust `chunkedBySeparator` consumption in `parseSongItem`. |
| Wrong artist (shows album or view count) | Byline group order changed; artist assumed to be the **first** group | Same as above. |
| Songs play but stream dies / 403 mid-song | **Not InnerTube** — stream resolution is NewPipe in `MediaService` | See the `playback-and-queue` skill. |
| Suggestions broken but search fine | Suggestions come from **NewPipe**, not InnerTube | Check `NewPipeRepository.getSearchSuggestions` / `DownloaderImpl` (429 → ReCaptcha); consider updating the NewPipe Extractor dependency. |

## Extending: adding a new InnerTube endpoint

1. Add the URL constant to `InnerTubeRepository`'s companion object.
2. Add a body builder to `HttpHelpers.kt` — copy the `context.client` block from an existing builder (WEB_REMIX + `CLIENT_VERSION` + `hl`/`gl`).
3. POST with `contentType(Json)` + `applyHeaders()`, decode to `JsonObject`.
4. Write a parser in `core/parsers/` using the `InnerTubeExtensions` helpers; return a plain domain model (`core/model/`). Wrap the repository call in try/catch returning an empty result, matching the existing functions.
5. Capture a real response (Step 1 above) **before** writing the parser; never guess key paths.
