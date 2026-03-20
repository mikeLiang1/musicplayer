package org.example.project.core.service

import android.content.Intent
import androidx.annotation.OptIn
import androidx.core.net.toUri
import androidx.media3.common.Player
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
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

        val player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(DefaultMediaSourceFactory(resolvingDataSourceFactory))
            .build()


        // Listen for player ended events to trigger queue advancement
        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    // When a song ends, advance to the next song in the queue
                    serviceScope.launch {
                        queueManager.playNext()
                    }
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
        // Callback implementation can be added here for Android Auto support
        // For now, we're using the default implementation
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
