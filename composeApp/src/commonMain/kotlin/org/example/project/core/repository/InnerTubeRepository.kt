package org.example.project.core.repository

import org.example.project.core.helper.findFlexText
import org.example.project.core.helper.findObjectWithKey
import org.example.project.core.helper.getRunsText
import org.example.project.core.helper.parseTimeToMillis

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import org.example.project.core.model.Song
import org.example.project.core.model.SongPage

class InnerTubeRepository(private val client: HttpClient) {

    // ─────────────────────────────────────────────────────────────
    // QUEUE / RADIO
    // ─────────────────────────────────────────────────────────────

    suspend fun getRecommendations(url: String): SongPage = withContext(Dispatchers.IO) {
        val videoId = extractVideoId(url) ?: return@withContext SongPage(emptyList(), null)
        try {
            val response: JsonObject = client.post(NEXT_URL) {
                contentType(ContentType.Application.Json)
                applyHeaders()
                setBody(buildNextBody(videoId))
            }.body()
            parseQueuePage(response)
        } catch (e: Exception) {
            println("getRecommendations error: ${e.message}")
            SongPage(emptyList(), null)
        }
    }

    suspend fun getMoreRecommendations(token: String): SongPage = withContext(Dispatchers.IO) {
        try {
            val response: JsonObject = client.post(NEXT_URL) {
                contentType(ContentType.Application.Json)
                applyHeaders()
                setBody(buildContinuationBody(token))
            }.body()
            parseQueuePage(response)
        } catch (e: Exception) {
            println("getMoreRecommendations error: ${e.message}")
            SongPage(emptyList(), null)
        }
    }

    // ─────────────────────────────────────────────────────────────
    // SEARCH
    // ─────────────────────────────────────────────────────────────

    suspend fun searchSongs(query: String): SongPage = withContext(Dispatchers.IO) {
        try {
            val response: JsonObject = client.post(SEARCH_URL) {
                contentType(ContentType.Application.Json)
                applyHeaders()
                setBody(buildSearchBody(query))
            }.body()
            parseSearchPage(response)
        } catch (e: Exception) {
            println("searchSongs error: ${e.message}")
            SongPage(emptyList(), null)
        }
    }

    suspend fun searchMoreSongs(token: String): SongPage = withContext(Dispatchers.IO) {
        try {
            val response: JsonObject = client.post(SEARCH_URL) {
                contentType(ContentType.Application.Json)
                applyHeaders()
                setBody(buildContinuationBody(token))
            }.body()
            parseSearchPage(response)
        } catch (e: Exception) {
            println("searchMoreSongs error: ${e.message}")
            SongPage(emptyList(), null)
        }
    }

    // ─────────────────────────────────────────────────────────────
    // REQUEST BUILDERS
    // ─────────────────────────────────────────────────────────────

    private fun HttpRequestBuilder.applyHeaders() {
        header("X-YouTube-Client-Name", "67")
        header("X-YouTube-Client-Version", CLIENT_VERSION)
        header(
            "User-Agent",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
        )
        header("Referer", "https://music.youtube.com/")
        header("Origin", "https://music.youtube.com")
    }

    private fun buildNextBody(videoId: String): JsonObject = buildJsonObject {
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
        put("params", "wAEB") // returns full radio queue
    }

    private fun buildSearchBody(query: String): JsonObject = buildJsonObject {
        putJsonObject("context") {
            putJsonObject("client") {
                put("clientName", "WEB_REMIX")
                put("clientVersion", CLIENT_VERSION)
                put("hl", "en")
                put("gl", "US")
            }
        }
        put("query", query)
        put("params", "EgWKAQIIAWoKEAkQBRAKEAMQBA%3D%3D") // music songs filter
    }

    // Shared between queue pagination and search pagination
    private fun buildContinuationBody(token: String): JsonObject = buildJsonObject {
        putJsonObject("context") {
            putJsonObject("client") {
                put("clientName", "WEB_REMIX")
                put("clientVersion", CLIENT_VERSION)
                put("hl", "en")
                put("gl", "US")
            }
        }
        put("continuation", token)
    }

    // ─────────────────────────────────────────────────────────────
    // QUEUE PARSING
    // ─────────────────────────────────────────────────────────────

    private fun parseQueuePage(root: JsonObject): SongPage {
        val playlistPanel = root.findObjectWithKey("playlistPanelRenderer")
            ?: return SongPage(emptyList(), null)

        val contents = playlistPanel["contents"]?.jsonArray
            ?: return SongPage(emptyList(), null)

        val songs = contents.mapNotNull { parseQueueItem(it.jsonObject) }

        val token = playlistPanel["continuations"]
            ?.jsonArray
            ?.firstOrNull()
            ?.jsonObject
            ?.get("nextRadioContinuationData")
            ?.jsonObject
            ?.get("continuation")
            ?.jsonPrimitive
            ?.content

        return SongPage(songs, token)
    }

    private fun parseQueueItem(item: JsonObject): Song? {
        val renderer = item["playlistPanelVideoRenderer"]?.jsonObject
            ?: return null

        val videoId = renderer["videoId"]?.jsonPrimitive?.content
            ?: return null

        return Song(
            uniqueId = videoId,
            url = "https://www.youtube.com/watch?v=$videoId",
            title = renderer["title"]?.jsonObject.getRunsText(),
            artist = renderer["longBylineText"]?.jsonObject
                .getRunsText()
                .split(" • ")
                .first(),
            thumbnailUrl = renderer["thumbnail"]
                ?.jsonObject
                ?.get("thumbnails")
                ?.jsonArray
                ?.lastOrNull()
                ?.jsonObject
                ?.get("url")
                ?.jsonPrimitive
                ?.content,
            duration = renderer["lengthText"]
                ?.jsonObject
                ?.get("runs")
                ?.jsonArray
                ?.firstOrNull()
                ?.jsonObject
                ?.get("text")
                ?.jsonPrimitive
                ?.content
                ?.parseTimeToMillis() ?: 0L
        )
    }

    // ─────────────────────────────────────────────────────────────
    // SEARCH PARSING
    // ─────────────────────────────────────────────────────────────

    private fun parseSearchPage(root: JsonObject): SongPage {
        val shelf = root.findObjectWithKey("musicShelfRenderer")
            ?: return SongPage(emptyList(), null)

        val contents = shelf["contents"]?.jsonArray
            ?: return SongPage(emptyList(), null)

        val songs = contents.mapNotNull { item ->
            val renderer = item.jsonObject["musicResponsiveListItemRenderer"]?.jsonObject
                ?: return@mapNotNull null
            parseSearchItem(renderer)
        }

        val token = shelf["continuations"]
            ?.jsonArray
            ?.firstOrNull()
            ?.jsonObject
            ?.get("nextContinuationData")
            ?.jsonObject
            ?.get("continuation")
            ?.jsonPrimitive
            ?.content

        return SongPage(songs, token)
    }

    private fun parseSearchItem(renderer: JsonObject): Song? {
        val videoId = renderer
            .findObjectWithKey("watchEndpoint")
            ?.get("videoId")
            ?.jsonPrimitive
            ?.content
            ?: return null

        val columns = renderer["flexColumns"]?.jsonArray ?: return null

        val title = columns.getOrNull(0)?.jsonObject.findFlexText()
        val artist = columns.getOrNull(1)?.jsonObject.findFlexText()
        val durationText = columns.getOrNull(2)?.jsonObject.findFlexText()

        val thumbnail = renderer["thumbnail"]
            ?.jsonObject
            ?.findObjectWithKey("thumbnails")
            ?.let { obj ->
                // findObjectWithKey returns JsonObject but thumbnails is an array
                // so fall back to searching for the array directly
                null
            }
            ?: renderer
                .findObjectWithKey("musicThumbnailRenderer")
                ?.get("thumbnail")
                ?.jsonObject
                ?.get("thumbnails")
                ?.jsonArray
                ?.lastOrNull()
                ?.jsonObject
                ?.get("url")
                ?.jsonPrimitive
                ?.content

        return Song(
            uniqueId = videoId,
            url = "https://www.youtube.com/watch?v=$videoId",
            title = title ?: return null,
            artist = artist ?: "Unknown",
            thumbnailUrl = thumbnail,
            duration = durationText?.parseTimeToMillis() ?: 0L
        )
    }

    // ─────────────────────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────────────────────

    private fun extractVideoId(url: String): String? = when {
        url.contains("v=") ->
            url.substringAfter("v=").substringBefore("&").takeIf { it.isNotBlank() }
        url.contains("youtu.be/") ->
            url.substringAfter("youtu.be/").substringBefore("?").takeIf { it.isNotBlank() }
        url.length == 11 -> url // raw video ID passed directly
        else -> null
    }

    companion object {
        private const val NEXT_URL = "https://music.youtube.com/youtubei/v1/next"
        private const val SEARCH_URL = "https://music.youtube.com/youtubei/v1/search"

        // Update when YouTube starts rejecting requests (check DevTools → Network → clientVersion)
        const val CLIENT_VERSION = "1.20241015.01.00"
    }
}
