package org.example.project.core.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.services.youtube.YoutubeService

class NewPipeRepository {
    private val youtubeService: YoutubeService =
        ServiceList.YouTube as YoutubeService

    // ─────────────────────────────────────────────────────────────
    // STREAM URL — NewPipe handles decryption, format selection
    // ─────────────────────────────────────────────────────────────

    suspend fun getStreamUrl(url: String): String? = withContext(Dispatchers.IO) {
        try {
            val extractor = youtubeService.getStreamExtractor(url)
            extractor.fetchPage()

            // Pick the highest-bitrate audio stream regardless of codec.
            // ExoPlayer handles Opus fine, and Opus (~160kbps) beats the
            // M4A/AAC ceiling (~128kbps) on YouTube, so don't filter by format.
            extractor.audioStreams
                .maxByOrNull { it.bitrate }
                ?.content
        } catch (e: Exception) {
            println("getStreamUrl error: ${e.message}")
            null
        }
    }

    // ─────────────────────────────────────────────────────────────
    // SUGGESTIONS — NewPipe works fine, no reason to switch
    // ─────────────────────────────────────────────────────────────

    suspend fun getSearchSuggestions(query: String): List<String> = withContext(Dispatchers.IO) {
        try {
            youtubeService.suggestionExtractor.suggestionList(query)
        } catch (e: Exception) {
            println("getSearchSuggestions error: ${e.message}")
            emptyList()
        }
    }
}
