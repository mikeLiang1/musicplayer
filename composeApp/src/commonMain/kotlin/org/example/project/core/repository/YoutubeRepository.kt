package org.example.project.core.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.example.project.core.model.Song
import org.schabi.newpipe.extractor.Page
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.search.SearchExtractor
import org.schabi.newpipe.extractor.stream.StreamInfoItem

class YouTubeRepository {

    // YouTube is Service ID 0 in NewPipe
    private val youtubeService = ServiceList.YouTube
    private var lastPage: Page? = null

    suspend fun searchSongs(query: String): List<Song> {
        return withContext(Dispatchers.IO) {
            // 1. Get the extractor for search
            // Filters for music: "music_songs", "music_videos", "music_albums", "music_playlists"
            val extractor: SearchExtractor = youtubeService.getSearchExtractor(
                query,
                listOf("music_songs"),
                null
            )

            // 2. Fetch the data from YouTube
            extractor.fetchPage()

            // 3. Store the next page object for pagination
            val initialPage = extractor.initialPage
            lastPage = initialPage.nextPage

            // 4. Map items to your Song model
            initialPage.items
                .filterIsInstance<StreamInfoItem>()
                .map { item ->
                    Song(
                        url = item.url,
                        title = item.name,
                        artist = item.uploaderName ?: "Unknown",
                        thumbnailUrl = item.thumbnails.firstOrNull()?.url,
                        duration = (item.duration * 1000).millisToDuration()
                    )
                }
        }
    }

    suspend fun searchMoreSongs(query: String): List<Song> {
        val currentPage = lastPage ?: return emptyList()

        return withContext(Dispatchers.IO) {
            // To get more items, we need the same extractor setup
            val extractor = youtubeService.getSearchExtractor(query, listOf("music_songs"), null)

            // Fetch the specific page
            val nextPageData = extractor.getPage(currentPage)
            lastPage = nextPageData.nextPage

            nextPageData.items
                .filterIsInstance<StreamInfoItem>()
                .map { item ->
                    Song(
                        url = item.url,
                        title = item.name,
                        artist = item.uploaderName ?: "Unknown",
                        thumbnailUrl = item.thumbnails.firstOrNull()?.url,
                        duration = (item.duration * 1000).millisToDuration()
                    )
                }
        }
    }

    suspend fun getSearchSuggestion(query: String): List<String> {
        return withContext(Dispatchers.IO) {
            // NewPipe has a specific suggestion extractor
            youtubeService.suggestionExtractor.suggestionList(query)
        }
    }

    suspend fun getStreamUrl(url: String): String? {
        return withContext(Dispatchers.IO) {
            // 1. Get the stream extractor for a specific URL
            val extractor = youtubeService.getStreamExtractor(url)

            // 2. Fetch data (this gets the actual video/audio links)
            extractor.fetchPage()

            // 3. Filter for audio streams and pick the best one
            // Usually, M4A 128kbps is best for compatibility/size
            val bestAudio = extractor.audioStreams
                .filter { it?.format?.name?.lowercase() == "m4a" }
                .maxByOrNull { it.bitrate }
                ?: extractor.audioStreams.firstOrNull()

            bestAudio?.url
        }
    }
    private fun Long.millisToDuration(): String {
        val totalSeconds = this / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60

        return "${minutes}:${seconds.toString().padStart(2, '0')}"
    }
}
