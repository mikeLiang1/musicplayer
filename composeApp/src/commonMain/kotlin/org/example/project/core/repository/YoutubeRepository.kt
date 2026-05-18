package org.example.project.core.repository

import androidx.core.net.toUri
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

    private var lastRadioPage: Page? = null


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
                        thumbnailUrl = item.thumbnails.maxByOrNull { it.width * it.height }?.url,
                        duration = item.duration * 1000
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
                        thumbnailUrl = item.thumbnails.maxByOrNull { it.width * it.height }?.url,
                        duration = item.duration * 1000
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

    suspend fun getPlaylistRadio(url: String): List<Song> {
        return withContext(Dispatchers.IO) {
            // 1. Get the stream extractor for a specific URL
            val videoId = url.toUri().getQueryParameter("v")
            val radioUrl = "https://music.youtube.com/watch?v=$videoId&list=RDAMVM$videoId"
            val extractor = youtubeService.getPlaylistExtractor(radioUrl)

            // 2. Fetch data (this gets the actual video/audio links)
            extractor.fetchPage()
            val initialPage = extractor.initialPage
            lastRadioPage = initialPage.nextPage

            // 3. Filter for audio streams and pick the best one
            // Usually, M4A 128kbps is best for compatibility/size
            initialPage.items.filterIsInstance<StreamInfoItem>().map { item ->
                Song(
                    url = item.url,
                    title = item.name,
                    artist = item.uploaderName ?: "Unknown",
                    thumbnailUrl = item.thumbnails.maxByOrNull { it.width * it.height }?.url,
                    duration = item.duration * 1000
                )
            }
        }
    }

    suspend fun getNextRadioSongs(url: String): List<Song> {
        val currentPage = lastRadioPage ?: return emptyList()
        return withContext(Dispatchers.IO) {
            // 1. Get the stream extractor for a specific URL
            val videoId = url.toUri().getQueryParameter("v")
            val radioUrl = "https://music.youtube.com/watch?v=$videoId&list=RDAMVM$videoId"
            val extractor = youtubeService.getPlaylistExtractor(radioUrl)

            val nextPageData = extractor.getPage(currentPage)
            lastRadioPage = nextPageData.nextPage

            // 3. Filter for audio streams and pick the best one
            // Usually, M4A 128kbps is best for compatibility/size
            nextPageData.items.filterIsInstance<StreamInfoItem>().map { item ->
                Song(
                    url = item.url,
                    title = item.name,
                    artist = item.uploaderName ?: "Unknown",
                    thumbnailUrl = item.thumbnails.maxByOrNull { it.width * it.height }?.url,
                    duration = item.duration * 1000
                )
            }
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

            bestAudio?.content
        }
    }

}
