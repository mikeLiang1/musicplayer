package org.example.project.core.service

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
import org.example.project.core.helper.toMediaItem
import org.example.project.core.manager.QueueManager
import org.example.project.core.repository.SavedDataRepository
import org.example.project.core.repository.YouTubeRepository
import org.koin.android.ext.android.inject

@OptIn(UnstableApi::class)
class MediaService : MediaLibraryService() {

    companion object {
        private const val CACHE_DURATION = 60 * 60 * 1000L
    }

    private var mediaSession: MediaLibrarySession? = null

    private val repo by inject<SavedDataRepository>()
    private val youtubeRepository by inject<YouTubeRepository>()
    private val queueManager by inject<QueueManager>()

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val urlCache = mutableMapOf<String, Pair<String, Long>>()

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()

        val resolvingDataSourceFactory = ResolvingDataSource.Factory(
            DefaultHttpDataSource.Factory()
        ) { dataSpec ->
            val youtubeId = dataSpec.uri.toString() // This is the YouTube ID

            val cached = urlCache[youtubeId]
            val streamUrl =
                if (cached != null && System.currentTimeMillis() - cached.second < CACHE_DURATION) {
                    cached.first // Use cached URL
                } else {
                    // Fetch fresh URL
                    runBlocking(Dispatchers.IO) {
                        try {
                            youtubeRepository.getStreamUrl(youtubeId)?.also { url ->
                                urlCache[youtubeId] = url to System.currentTimeMillis()
                            } ?: ""
                        } catch (e: Exception) {
                            Log.e("MediaService", "Failed to resolve: $youtubeId", e)
                            ""
                        }
                    }
                }

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
                    // TODO: I think this is called when playlist ends
//                    serviceScope.launch {
//                        queueManager.playNext()
//                    }
                }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO) {
                    queueManager.playNext(fromAutoAdvanced = true)
                }
            }
        })

        val forwardingPlayer = QueueForwardingPlayer(player, queueManager)
        mediaSession = MediaLibrarySession.Builder(this, forwardingPlayer, PlayerCallback())
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

    private fun saveData() {
        val currentPos = mediaSession?.player?.currentPosition

        mediaSession?.player?.clearMediaItems()

        if (currentPos != null) {
            serviceScope.launch {
                repo.savePosition(currentPos)
            }
        }
    }
}
