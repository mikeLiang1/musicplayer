package org.example.project.core.repository

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import org.example.project.core.model.Song

class YouTubeMusicRepository() {

    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                prettyPrint = true  // This makes the output readable
                isLenient = true
            })
        }
    }

    // ─────────────────────────────────────────────────────────────
    // PUBLIC API
    // ─────────────────────────────────────────────────────────────

    suspend fun getNextRecommendations(sentUrl: String): List<Song> = withContext(Dispatchers.IO) {
        val videoId = extractVideoId(sentUrl) ?: return@withContext emptyList()

        try {
            val response: JsonObject = client.post(INNERTUBE_NEXT_URL) {
                contentType(ContentType.Application.Json)
                applyInnerTubeHeaders()
                setBody(buildNextRequestBody(videoId))
            }.body()

            parseQueue(response)
        } catch (e: Exception) {
            println("InnerTube error: ${e.message}")
            emptyList()
        }
    }

    // ─────────────────────────────────────────────────────────────
    // REQUEST BUILDING
    // ─────────────────────────────────────────────────────────────

    private fun HttpRequestBuilder.applyInnerTubeHeaders() {
        header("X-YouTube-Client-Name", "67")
        header("X-YouTube-Client-Version", CLIENT_VERSION)
        header(
            "User-Agent",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
        )
        header("Referer", "https://music.youtube.com/")
        header("Origin", "https://music.youtube.com")
    }

    private fun buildNextRequestBody(videoId: String): JsonObject = buildJsonObject {
        putJsonObject("context") {
            putJsonObject("client") {
                put("clientName", "WEB_REMIX")
                put("clientVersion", CLIENT_VERSION)
                put("hl", "en")
                put("gl", "US")
                put(
                    "userAgent",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36,gzip(gfe)"
                )
                put("platform", "DESKTOP")
            }
        }
        put("videoId", videoId)
        put("playlistId", "RDAMVM$videoId")
        put("params", "wAEB") // tells YouTube to return the full radio queue
    }

    // ─────────────────────────────────────────────────────────────
    // PARSING
    // ─────────────────────────────────────────────────────────────

    private fun parseQueue(root: JsonObject): List<Song> {
        val playlistPanel = root.findObjectWithKey("playlistPanelRenderer")
            ?: return emptyList()

        val contents = playlistPanel["contents"]?.jsonArray
            ?: return emptyList()

        return contents.mapNotNull { item ->
            parseSongItem(item.jsonObject)
        }
    }

    private fun parseSongItem(item: JsonObject): Song? {
        val renderer = item["playlistPanelVideoRenderer"]?.jsonObject
            ?: return null

        val videoId = renderer["videoId"]?.jsonPrimitive?.content
            ?: return null

        return Song(
            uniqueId = videoId,
            url = "https://www.youtube.com/watch?v=$videoId",
            title = renderer["title"]?.jsonObject.getRunsText(),
            artist = renderer["longBylineText"]?.jsonObject.getRunsText(),
            thumbnailUrl = extractBestThumbnail(renderer),
            duration = extractDuration(renderer)
        )
    }

    private fun extractBestThumbnail(renderer: JsonObject): String? {
        return renderer["thumbnail"]
            ?.jsonObject
            ?.get("thumbnails")
            ?.jsonArray
            ?.lastOrNull() // last = highest resolution
            ?.jsonObject
            ?.get("url")
            ?.jsonPrimitive
            ?.content
    }

    private fun extractDuration(renderer: JsonObject): Long {
        // lengthText looks like "3:45" or "1:02:30"
        val timeString = renderer["lengthText"]
            ?.jsonObject
            ?.get("runs")
            ?.jsonArray
            ?.firstOrNull()
            ?.jsonObject
            ?.get("text")
            ?.jsonPrimitive
            ?.content
            ?: return 0L

        return timeString.parseTimeToMillis()
    }

    // ─────────────────────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────────────────────

    private fun extractVideoId(url: String): String? {
        return when {
            url.contains("v=") -> url.substringAfter("v=").substringBefore("&").takeIf { it.isNotBlank() }
            url.contains("youtu.be/") -> url.substringAfter("youtu.be/").substringBefore("?").takeIf { it.isNotBlank() }
            url.length == 11 -> url // raw video ID passed directly
            else -> null
        }
    }

    companion object {
        // No API key needed — YouTube removed this requirement
        private const val INNERTUBE_NEXT_URL = "https://music.youtube.com/youtubei/v1/next"

        // Update this periodically by checking a real YTM network request
        // in your browser's DevTools → Network tab → look for clientVersion
        private const val CLIENT_VERSION = "1.20241015.01.00"
    }
}

// ─────────────────────────────────────────────────────────────────
// EXTENSION FUNCTIONS (top-level, reusable across the project)
// ─────────────────────────────────────────────────────────────────

/**
 * Recursively searches a JsonElement tree for an object containing [key],
 * and returns the value at that key as a JsonObject.
 *
 * Useful for InnerTube responses where nesting depth changes frequently.
 */
fun JsonElement.findObjectWithKey(key: String): JsonObject? {
    if (this is JsonObject) {
        if (this.containsKey(key)) return this[key]?.jsonObject
        for (value in this.values) {
            val found = value.findObjectWithKey(key)
            if (found != null) return found
        }
    } else if (this is JsonArray) {
        for (element in this) {
            val found = element.findObjectWithKey(key)
            if (found != null) return found
        }
    }
    return null
}

/**
 * Extracts plain text from YouTube's "runs" format:
 * { "runs": [ { "text": "Artist" }, { "text": " • " }, { "text": "Album" } ] }
 */
fun JsonObject?.getRunsText(): String {
    val runs = this?.get("runs")?.jsonArray ?: return "Unknown"
    return runs.joinToString("") {
        it.jsonObject["text"]?.jsonPrimitive?.content ?: ""
    }
}

/**
 * Parses a YouTube duration string ("3:45", "1:02:30") into milliseconds.
 */
fun String.parseTimeToMillis(): Long {
    return try {
        val parts = trim().split(":").map { it.toLong() }
        when (parts.size) {
            2 -> (parts[0] * 60 + parts[1]) * 1000
            3 -> (parts[0] * 3600 + parts[1] * 60 + parts[2]) * 1000
            else -> 0L
        }
    } catch (e: NumberFormatException) {
        0L
    }
}
