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
import io.ktor.client.request.accept
import org.example.project.core.helper.applyHeaders
import org.example.project.core.helper.buildContinuationBody
import org.example.project.core.helper.buildNextBody
import org.example.project.core.helper.buildPlayerBody
import org.example.project.core.helper.buildSearchBody
import org.example.project.core.helper.extractVideoId
import org.example.project.core.model.SearchResult
import org.example.project.core.model.SongPage
import org.example.project.core.parsers.parsePlayerResponse
import org.example.project.core.parsers.parseQueuePage
import org.example.project.core.parsers.parseSearchPage

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
    /**
     * Resolves a YouTube video ID to an audio stream URL.
     *
     * Strategy (inspired by Metrolist):
     * 1. Call the InnerTube /player endpoint (WEB_REMIX client, same as search/next)
     * 2. Parse adaptiveFormats for the best-quality audio-only stream
     * 3. If the format has a direct URL, use it
     * 4. If it has a signatureCipher, decode it (parse URL params, handle 's' signature)
     * 5. If both fail, fall back to NewPipe extractor for signature deobfuscation
     */
    suspend fun getStreamUrl(videoId: String): String? = withContext(Dispatchers.IO) {
        try {
            val response: JsonObject = client.post(PLAYER_URL) {
                contentType(ContentType.Application.Json)
                applyHeaders()
                setBody(buildPlayerBody(videoId))
                accept(ContentType.Application.Json)
            }.body()

            // Parse the player response to get the best audio stream URL
            parsePlayerResponse(response)
        } catch (e: Exception) {
            println("getStreamUrl error: ${e.message}")
            null
        }
    }

    companion object {
        private const val NEXT_URL = "https://music.youtube.com/youtubei/v1/next"
        private const val SEARCH_URL = "https://music.youtube.com/youtubei/v1/search"
        private const val PLAYER_URL = "https://music.youtube.com/youtubei/v1/player"

        // Update when YouTube starts rejecting requests (check DevTools → Network → clientVersion)
        const val CLIENT_VERSION = "1.20241015.01.00"
    }
}
