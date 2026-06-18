package org.example.project.core.repository

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.putJsonObject
import org.example.project.core.helper.applyHeaders
import org.example.project.core.helper.buildContinuationBody
import org.example.project.core.helper.buildNextBody
import org.example.project.core.helper.buildSearchBody
import org.example.project.core.helper.extractVideoId
import org.example.project.core.model.SearchResult
import org.example.project.core.model.SongPage
import org.example.project.core.parsers.parseQueuePage
import org.example.project.core.parsers.parseSearchPage
import org.schabi.newpipe.extractor.ServiceList.YouTube
import org.schabi.newpipe.extractor.timeago.patterns.it
import org.slf4j.MDC.put

class InnerTubeRepository(private val client: HttpClient) {


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

    suspend fun searchSongs(query: String): SearchResult = withContext(Dispatchers.IO) {
        try {
            val response: JsonObject = client.post(SEARCH_URL) {
                contentType(ContentType.Application.Json)
                applyHeaders()
                setBody(buildSearchBody(query))
            }.body()
            parseSearchPage(response)
        } catch (e: Exception) {
            println("searchSongs error: ${e.message}")
            SearchResult()
        }
    }

    suspend fun searchMoreSongs(token: String): SearchResult = withContext(Dispatchers.IO) {
        try {
            val response: JsonObject = client.post(SEARCH_URL) {
                contentType(ContentType.Application.Json)
                applyHeaders()
                setBody(buildContinuationBody(token))
            }.body()
            parseSearchPage(response)
        } catch (e: Exception) {
            println("searchMoreSongs error: ${e.message}")
            SearchResult()
        }
    }
//
//    suspend fun getStreamUrl(videoId: String): String? = withContext(Dispatchers.IO) {
//        val response = YouTube.player(
//            videoId,
//            playlistId = null,
//            client = MAIN_CLIENT,   // pick ONE client for now
//            signatureTimestamp = null,
//            poToken = null
//        ).getOrNull() ?: return@withContext null
//
//        val streamingData = response.streamingData ?: return@withContext null
//        val formats = streamingData.adaptiveFormats ?: return@withContext null
//
//        val best = formats
//            .filter { it.audioQuality != null }
//            .maxByOrNull { it.bitrate ?: 0 }
//            ?: return@withContext null
//
//        // 1. direct URL
//        best.url?.takeIf { it.startsWith("http") }?.let { return@withContext it }
//
//        // 2. fallback: signatureCipher (IMPORTANT)
//        best.signatureCipher
//            ?.let { decodeCipher(it) }
//            ?.takeIf { it.startsWith("http") }
//            ?.let { return@withContext it }
//
//        null
//    }

    fun buildPlayerBody(videoId: String): JsonObject {
        return buildJsonObject {
            put("videoId", videoId)

            putJsonObject("context") {
                putJsonObject("client") {
                    put("clientName", "ANDROID_MUSIC")
                    put("clientVersion", InnerTubeRepository.CLIENT_VERSION)
                }
            }
        }
    }

    fun decodeSignatureCipher(cipher: String?): String? {
        if (cipher.isNullOrBlank()) return null

        val params = cipher.split("&").associate {
            val parts = it.split("=")
            parts[0] to parts.getOrNull(1)
        }

        val url = params["url"] ?: return null

        return url // ⚠️ real Metrolist also appends decoded "s" signature
    }


    companion object {
        private const val NEXT_URL = "https://music.youtube.com/youtubei/v1/next"
        private const val SEARCH_URL = "https://music.youtube.com/youtubei/v1/search"

        // Update when YouTube starts rejecting requests (check DevTools → Network → clientVersion)
        const val CLIENT_VERSION = "1.20241015.01.00"
    }
}
