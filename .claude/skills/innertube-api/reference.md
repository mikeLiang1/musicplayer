# InnerTube reference — exact JSON key-paths and request bodies

Extracted verbatim from the parsers on 2026-07-02 (including the uncommitted working-tree
version of `SearchParser.kt`). When YouTube changes a shape, update both the parser and this file.

## Request bodies (`core/helper/HttpHelpers.kt`)

### /search — `buildSearchBody(query)`
```json
{
  "context": { "client": { "clientName": "WEB_REMIX", "clientVersion": "<CLIENT_VERSION>", "hl": "en", "gl": "US" } },
  "query": "<query>",
  "params": "EgWKAQIIAWoKEAkQBRAKEAMQBA%3D%3D"
}
```
`params` = "music songs" filter. Removing it returns mixed shelves (Albums/Artists/Videos…).

### /next — `buildNextBody(videoId)`
```json
{
  "context": { "client": { "clientName": "WEB_REMIX", "clientVersion": "<CLIENT_VERSION>", "hl": "en", "gl": "US", "userAgent": "<desktop Chrome UA>,gzip(gfe)", "platform": "DESKTOP" } },
  "videoId": "<id>",
  "playlistId": "RDAMVM<id>",
  "params": "wAEB"
}
```
`RDAMVM<videoId>` = the "radio" playlist for that video; `wAEB` = return the full radio queue.

### /player — `buildPlayerBody(videoId)` (unused in production)
```json
{
  "context": { "client": { "clientName": "WEB_REMIX", "clientVersion": "<CLIENT_VERSION>", "hl": "en", "gl": "US", "platform": "DESKTOP" } },
  "videoId": "<id>",
  "contentCheckOk": true,
  "racyCheckOk": true
}
```

### Continuations (shared, search + queue) — `buildContinuationBody(token)`
```json
{
  "context": { "client": { "clientName": "WEB_REMIX", "clientVersion": "<CLIENT_VERSION>", "hl": "en", "gl": "US" } },
  "continuation": "<token>"
}
```

### Headers on every request — `applyHeaders()`
```
X-YouTube-Client-Name: 67
X-YouTube-Client-Version: <CLIENT_VERSION>
User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36
Referer: https://music.youtube.com/
Origin: https://music.youtube.com
```
Plus from `networkModule` `defaultRequest`: `Accept: application/json`,
`Accept-Language: en-US,en;q=0.9`, `Cache-Control: no-cache`.

## Search response — paths walked by `parseSearchPage` / `parseSongItem`

### Initial response
```
(root)
└── … findObjectWithKey("sectionListRenderer")            ← depth-first, wrapper levels don't matter
    └── contents[]
        └── musicShelfRenderer
            ├── title.runs[0].text                        ← must equal "Songs" | "Albums" | "Artists" (English, via hl=en)
            ├── contents[]                                ← items, one per row
            └── continuations[0].nextContinuationData.continuation   ← pagination token
```

### Per song row — `parseSongItem`
```
musicResponsiveListItemRenderer
├── … findObjectWithKey("watchEndpoint").videoId          ← REQUIRED; row skipped if absent
├── flexColumns[0].musicResponsiveListItemFlexColumnRenderer.text.runs[0].text    ← title (via getRuns())
├── flexColumns[1].musicResponsiveListItemFlexColumnRenderer.text.runs[]          ← byline
│       runs chunked on the " • " separator run:
│       group[0] = artist, group[1] = album, group[last] = duration "M:SS"
└── thumbnail.musicThumbnailRenderer.thumbnail.thumbnails[last].url               ← largest image
```
Output: `Song(uniqueId = videoId, url = "https://www.youtube.com/watch?v=<id>", …)`.

### Album / Artist rows (rare — songs filter usually excludes them)
```
musicTwoRowItemRenderer
├── title.runs[0].text
└── navigationEndpoint … findObjectWithKey("browseId")    ← WARNING: browseId is a string primitive;
                                                            findObjectWithKey does .jsonObject on it and
                                                            would throw. Latent bug, masked by try/catch.
```

### Continuation ("load more") response — different shape, handled FIRST
```
(root)
└── … findObjectWithKey("musicShelfContinuation")         ← actual path: continuationContents.musicShelfContinuation
    ├── contents[]                                        ← same musicResponsiveListItemRenderer rows
    └── continuations[0].nextContinuationData.continuation
```
No `sectionListRenderer`, no shelf title. If only this branch breaks: first page works,
pagination silently stops.

## Radio queue response (/next) — `parseQueuePage` / `parseQueueItem`

```
(root)
└── … findObjectWithKey("playlistPanelRenderer")
    ├── contents[]
    │   └── playlistPanelVideoRenderer
    │       ├── videoId                                   ← REQUIRED
    │       ├── title.runs[].text (joined)                ← getRunsText()
    │       ├── longBylineText.runs[].text (joined), then .split(" • ").first()  ← artist
    │       ├── thumbnail.thumbnails[last].url            ← note: NO musicThumbnailRenderer level here
    │       └── lengthText.runs[0].text                   ← "M:SS" → parseTimeToMillis()
    └── continuations[0].nextRadioContinuationData.continuation   ← parsed but currently unused
                                                                    (getMoreRecommendations has no callers)
```

## Player response (/player) — `parsePlayerResponse` (unused in production)

```
(root)
└── streamingData.adaptiveFormats[]
    ├── width          ← if present and > 0 → video format, skipped
    ├── mimeType       ← REQUIRED
    ├── url | signatureCipher | cipher                    ← one required
    ├── bitrate, audioQuality (AUDIO_QUALITY_HIGH/MEDIUM/LOW), audioChannels
```
Selection: quality tier ↓, channels ↓, bitrate ↓, codec (opus > mp4a).
`decodeSignatureCipher` handles only the trivial `sp/url/s` param case — it does NOT run
YouTube's player.js cipher ops. This is why production playback uses NewPipe instead.

## Dead code — do not resurrect

- `core/model/network/SearchResponse.kt`, `MusicResponsiveListItemRenderer.kt`, `Tabs.kt` — fully commented-out typed models, replaced by tree-walking.
- `core/model/network/Continuation.kt` — never imported.
- `SearchParser.parseByline()` — defined, never called.
- Spurious `import org.schabi.newpipe.extractor.timeago.patterns.it` in `SearchParser.kt` and `MusicPlayerManager.kt` — IDE auto-import accidents; safe to delete.
