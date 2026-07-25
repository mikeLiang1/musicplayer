package org.example.project.core.service

import android.app.PendingIntent
import android.content.Intent
import androidx.annotation.OptIn
import androidx.core.net.toUri
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.example.project.MainActivity
import org.example.project.core.helper.toMediaItem
import org.example.project.core.manager.PlaybackMode
import org.example.project.core.manager.QueueManager
import org.example.project.core.manager.QueueState
import org.example.project.core.repository.InnerTubeRepository
import org.example.project.core.repository.NewPipeRepository
import org.example.project.core.repository.PlaybackRepository
import org.koin.android.ext.android.inject
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

@OptIn(UnstableApi::class)
class MediaService : MediaLibraryService() {

    companion object {
        private const val CACHE_DURATION = 60 * 60 * 1000L
        private const val EXPIRY_SAFETY_MARGIN = 5 * 60 * 1000L
        private const val MAX_CACHE_SIZE = 100
    }

    private data class CachedUrl(val url: String, val expiresAt: Long)

    private var mediaSession: MediaLibrarySession? = null

    private val repo by inject<PlaybackRepository>()
    private val newPipeRepository by inject<NewPipeRepository>()

    private val innerTubeRepository by inject<InnerTubeRepository>()
    private val queueManager by inject<QueueManager>()

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val urlCache = ConcurrentHashMap<String, CachedUrl>()

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()

        val resolvingDataSourceFactory = ResolvingDataSource.Factory(
            DefaultHttpDataSource.Factory()
        ) { dataSpec ->
            val youtubeId = dataSpec.uri.toString() // This is the YouTube ID
            val streamUrl = resolveStreamUrl(youtubeId)

            // Swap the YouTube ID for the real Stream URL
            dataSpec.withUri(streamUrl.toUri())
        }

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()


        val player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(DefaultMediaSourceFactory(resolvingDataSourceFactory))
            .setAudioAttributes(audioAttributes, true)
            .build()


        // Listen for player ended events to trigger queue advancement
        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    handleQueueEnded()
                }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO) {
                    queueManager.playNext(fromAutoAdvanced = true)
                }
            }
        })

        val sessionActivityPendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                putExtra(MainActivity.EXTRA_OPEN_PLAYER, true)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_IMMUTABLE
        )

        val forwardingPlayer = QueueForwardingPlayer(player, queueManager)
        mediaSession = MediaLibrarySession.Builder(this, forwardingPlayer, PlayerCallback())
            .setSessionActivity(sessionActivityPendingIntent)
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? {
        return mediaSession
    }

    @OptIn(UnstableApi::class)
    override fun onTaskRemoved(rootIntent: Intent?) {
        pauseAllPlayersAndStopSelf()
        saveData()
        Log.d("Logging", "Task removed saved")
        mediaSession?.player?.clearMediaItems()
    }

    @UnstableApi
    private inner class PlayerCallback : MediaLibrarySession.Callback {

        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo
        ): MediaSession.ConnectionResult {
            return MediaSession.ConnectionResult.AcceptedResultBuilder(session).build()
        }

        override fun onPlaybackResumption(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            isForPlayback: Boolean
        ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
            val state = queueManager.queueState.value
            return Futures.immediateFuture(
                MediaSession.MediaItemsWithStartPosition(
                    state.playbackQueue.map { it.toMediaItem() },
                    state.playbackCurrentIndex,
                    0L
                )
            )
        }

        override fun onGetLibraryRoot(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<MediaItem>> {
            val root = MediaItem.Builder()
                .setMediaId("root")
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setIsPlayable(true)
                        .setTitle("Music Player")
                        .setIsBrowsable(true)
                        .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_MIXED)
                        .build()
                )
                .build()
            return Futures.immediateFuture(LibraryResult.ofItem(root, params))
        }

        override fun onGetChildren(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            parentId: String,
            page: Int,
            pageSize: Int,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
            val children = when (parentId) {
                "root" -> listOf(
                    browseNode("now_playing", "Now Playing", MediaMetadata.MEDIA_TYPE_PLAYLIST),
                    browseNode("queue", "Current Queue", MediaMetadata.MEDIA_TYPE_PLAYLIST)
                )

                "queue" -> queueManager.queueState.value.playbackQueue
                    .map { it.toMediaItem() }

                else -> emptyList()
            }
            return Futures.immediateFuture(LibraryResult.ofItemList(children, params))
        }
    }


    override fun onDestroy() {
        saveData()
        Log.d("Logging", "OnDestroy saved")
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }

    private fun browseNode(id: String, title: String, mediaType: Int) = MediaItem.Builder()
        .setMediaId(id)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(title)
                .setMediaType(mediaType)
                .setIsBrowsable(true)
                .setIsPlayable(false)
                .build()
        ).build()

    /**
     * ExoPlayer has no more media items to advance into. This never fires mid-queue —
     * onMediaItemTransition(REASON_AUTO) handles that — only at the true end of baseQueue
     * with an empty manual queue.
     */
    private fun handleQueueEnded() {
        val state = queueManager.queueState.value
        when (state.playbackMode) {
            PlaybackMode.REPEAT -> queueManager.restartFromBeginning()
            PlaybackMode.Infinite -> refillRadioQueue(state)
            PlaybackMode.OFF -> Unit // expected: playback stops at the end of the queue
        }
    }

    /**
     * Fetches another radio page seeded by the last song and appends it, then advances into
     * the first newly-added song. If the radio API only returns songs already in seenIds,
     * appendRadioSongs() dedupes them to nothing and playback simply stays ended — a known
     * limitation of the seenIds anti-repeat design, not re-seeded here.
     */
    private fun refillRadioQueue(state: QueueState) {
        val lastSong = state.baseQueue.lastOrNull() ?: return
        serviceScope.launch {
            try {
                val page = innerTubeRepository.getRecommendations(lastSong.url)
                if (page.songs.isNotEmpty()) {
                    queueManager.appendRadioSongs(page.songs)
                    queueManager.playNext()
                } else {
                    Log.e("MediaService", "Infinite radio refill returned no songs for: ${lastSong.url}")
                }
            } catch (e: Exception) {
                Log.e("MediaService", "Infinite radio refill failed for: ${lastSong.url}", e)
            }
        }
    }

    private fun resolveStreamUrl(youtubeId: String): String {
        purgeCache()

        val cached = urlCache[youtubeId]
        if (cached != null && System.currentTimeMillis() < cached.expiresAt) {
            return cached.url
        }

        // Fetch fresh URL, retrying once if the first attempt fails
        val resolved = fetchAndCacheStreamUrl(youtubeId) ?: fetchAndCacheStreamUrl(youtubeId)
        if (resolved != null) {
            return resolved
        }

        Log.e("MediaService", "Giving up resolving stream URL for: $youtubeId, skipping track")
        serviceScope.launch { queueManager.playNext() }
        throw IOException("Unable to resolve stream URL for $youtubeId")
    }

    private fun fetchAndCacheStreamUrl(youtubeId: String): String? = runBlocking(Dispatchers.IO) {
        try {
            newPipeRepository.getStreamUrl(youtubeId)?.also { url ->
                urlCache[youtubeId] = CachedUrl(url, parseExpiry(url))
            }
        } catch (e: Exception) {
            Log.e("MediaService", "Failed to resolve: $youtubeId", e)
            null
        }
    }

    private fun parseExpiry(url: String): Long {
        val expireSeconds = url.toUri().getQueryParameter("expire")?.toLongOrNull()
        return if (expireSeconds != null) {
            expireSeconds * 1000L - EXPIRY_SAFETY_MARGIN
        } else {
            System.currentTimeMillis() + CACHE_DURATION
        }
    }

    private fun purgeCache() {
        val now = System.currentTimeMillis()
        urlCache.entries.removeAll { it.value.expiresAt < now }

        val overflow = urlCache.size - MAX_CACHE_SIZE
        if (overflow > 0) {
            urlCache.entries
                .sortedBy { it.value.expiresAt }
                .take(overflow)
                .forEach { urlCache.remove(it.key) }
        }
    }

    private fun saveData() {
        val currentPos = mediaSession?.player?.currentPosition

        mediaSession?.player?.clearMediaItems()

        if (currentPos != null) {
            // Block until the write completes: this runs from onDestroy/onTaskRemoved, where the
            // process can be killed immediately afterward, so a fire-and-forget launch may be lost.
            runBlocking {
                repo.savePosition(currentPos)
            }
        }
    }
}
