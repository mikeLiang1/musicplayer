package org.example.project.core.parsers

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.example.project.core.helper.chunkedBySeparator
import org.example.project.core.helper.findObjectWithKey
import org.example.project.core.helper.getRuns
import org.example.project.core.helper.parseTimeToMillis
import org.example.project.core.model.Album
import org.example.project.core.model.Artist
import org.example.project.core.model.SearchResult
import org.example.project.core.model.Song
import org.schabi.newpipe.extractor.timeago.patterns.it

fun parseSearchPage(root: JsonObject): SearchResult {
    val sectionList =
        root.findObjectWithKey("sectionListRenderer") ?: return SearchResult()
    val shelves = sectionList["contents"]?.jsonArray ?: return SearchResult()

    val songs = mutableListOf<Song>()
    val albums = mutableListOf<Album>()
    val artists = mutableListOf<Artist>()
    var continuationToken: String? = null

    for (shelf in shelves) {
        val renderer = shelf.jsonObject["musicShelfRenderer"]?.jsonObject ?: continue
        val title = renderer["title"]?.jsonObject?.get("runs")?.jsonArray
            ?.firstOrNull()?.jsonObject?.get("text")?.jsonPrimitive?.content

        val items = renderer["contents"]?.jsonArray ?: continue

        when (title) {
            "Songs" -> songs.addAll(items.mapNotNull { parseSongItem(it.jsonObject) })
            "Albums" -> albums.addAll(items.mapNotNull { parseAlbumItem(it.jsonObject) })
            "Artists" -> artists.addAll(items.mapNotNull { parseArtistItem(it.jsonObject) })
        }
        if (continuationToken == null) {
            continuationToken = renderer["continuations"]
                ?.jsonArray
                ?.firstOrNull()
                ?.jsonObject
                ?.get("nextContinuationData")
                ?.jsonObject
                ?.get("continuation")
                ?.jsonPrimitive
                ?.content
        }
    }

    return SearchResult(songs, albums, artists, continuationToken)
}

fun parseSongItem(item: JsonObject): Song? {
    val renderer = item["musicResponsiveListItemRenderer"]?.jsonObject ?: return null
    val videoId = renderer.findObjectWithKey("watchEndpoint")?.get("videoId")?.jsonPrimitive?.content ?: return null

    // 1. Get the array, then iterate/access
    val columns = renderer["flexColumns"]?.jsonArray ?: return null

    // 2. Safely get the JsonElement, then convert to JsonObject
    val titleColumn = columns.getOrNull(0)?.jsonObject
    val artistColumn = columns.getOrNull(1)?.jsonObject

    // 3. Now use the getRuns() extension I gave you
    val title = titleColumn?.getRuns()?.firstOrNull()?.get("text")?.jsonPrimitive?.content

    val secondaryLine = artistColumn?.getRuns() ?: emptyList()
    val parts = secondaryLine.chunkedBySeparator()
    val artist = parts.firstOrNull()?.joinToString("") { it["text"]?.jsonPrimitive?.content ?: "" } ?: "Unknown"
    val album = parts.getOrNull(1)?.joinToString("") { it["text"]?.jsonPrimitive?.content ?: "" }

    val durationText = parts.lastOrNull()?.joinToString("") { it["text"]?.jsonPrimitive?.content ?: "" }

    // 4. Thumbnail
    val thumbnail = renderer["thumbnail"]?.jsonObject
        ?.get("musicThumbnailRenderer")?.jsonObject
        ?.get("thumbnail")?.jsonObject
        ?.get("thumbnails")?.jsonArray
        ?.lastOrNull()?.jsonObject
        ?.get("url")?.jsonPrimitive?.content

    return Song(
        uniqueId = videoId,
        url = "https://www.youtube.com/watch?v=$videoId",
        title = title ?: "Unknown Title",
        artist = artist,
        album = album,
        thumbnailUrl = thumbnail,
        duration = durationText?.parseTimeToMillis() ?: 0L
    )
}

// For Albums & Artists (Two Row Item)
fun parseAlbumItem(item: JsonObject): Album? {
    val renderer = item["musicTwoRowItemRenderer"]?.jsonObject ?: return null
    val title =
        renderer["title"]?.jsonObject?.get("runs")?.jsonArray?.firstOrNull()?.jsonObject?.get("text")?.jsonPrimitive?.content
            ?: return null
    val id = renderer["navigationEndpoint"]?.jsonObject?.findObjectWithKey("browseId")?.jsonPrimitive?.content ?: ""

    return Album(id, title, null, null)
}

fun parseArtistItem(item: JsonObject): Artist? {
    val renderer = item["musicTwoRowItemRenderer"]?.jsonObject ?: return null
    val name =
        renderer["title"]?.jsonObject?.get("runs")?.jsonArray?.firstOrNull()?.jsonObject?.get("text")?.jsonPrimitive?.content
            ?: return null
    val id = renderer["navigationEndpoint"]?.jsonObject?.findObjectWithKey("browseId")?.jsonPrimitive?.content ?: ""

    return Artist(id, name, null)
}

fun JsonObject.parseByline(): Pair<String, String?> {
    val runs = this["musicResponsiveListItemFlexColumnRenderer"]?.jsonObject
        ?.get("text")?.jsonObject
        ?.get("runs")?.jsonArray ?: return "Unknown" to null

    val parts = runs.mapNotNull { it.jsonObject["text"]?.jsonPrimitive?.content }
        .filter { it != " • " && it.isNotBlank() }

    return (parts.getOrNull(0) ?: "Unknown") to parts.getOrNull(1)
}
