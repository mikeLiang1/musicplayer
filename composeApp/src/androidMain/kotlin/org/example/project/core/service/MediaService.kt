package org.example.project.core.service

import android.content.Intent
import androidx.annotation.OptIn
import androidx.core.net.toUri
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
import org.example.project.core.repository.PlaybackRepository
import org.example.project.core.repository.YouTubeRepository
import org.koin.android.ext.android.inject

@OptIn(UnstableApi::class)
class MediaService : MediaLibraryService() {
    private var mediaSession: MediaLibrarySession? = null

    private val playbackRepository by inject<PlaybackRepository>()
    private val youtubeRepository by inject<YouTubeRepository>()

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)


    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        val resolvingDataSourceFactory = ResolvingDataSource.Factory(
            DefaultHttpDataSource.Factory()
        ) { dataSpec ->
            val mediaId = dataSpec.uri.toString() // This is the YouTube ID

            // Fetch the fresh stream URL synchronously (safe on background thread)
            val streamUrl = runBlocking { youtubeRepository.getStreamUrl(mediaId) } ?: ""

            // Swap the YouTube ID for the real Stream URL
            dataSpec.withUri(streamUrl.toUri())
        }
        val player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(DefaultMediaSourceFactory(resolvingDataSourceFactory)).build()
        mediaSession = MediaLibrarySession.Builder(this, player, PlayerCallback()).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? {
        return mediaSession
    }

    @OptIn(UnstableApi::class)
    override fun onTaskRemoved(rootIntent: Intent?) {
        pauseAllPlayersAndStopSelf()

        val currentPos = mediaSession?.player?.currentPosition
        val duration = mediaSession?.player?.duration

        mediaSession?.player?.clearMediaItems()

        if (currentPos != null && duration != null) {
            serviceScope.launch {
                playbackRepository.savePosition(currentPos, duration)
            }
        }

    }

    @UnstableApi
    private inner class PlayerCallback : MediaLibrarySession.Callback {

        /**
         * 2. PLAYBACK RESUMPTION
         * Triggered when Bluetooth/System-UI wants to resume playback after app death.
         */
//        override fun onPlaybackResumption(
//            mediaSession: MediaSession,
//            controller: MediaSession.ControllerInfo,
//            isForPlayback: Boolean
//        ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
//            val future = SettableFuture.create<MediaSession.MediaItemsWithStartPosition>()
//
//            serviceScope.launch {
//                val lastState = playbackRepository.playbackState.first()
//                val lastSongId = lastState.song?.url // YouTube URL saved in DataStore
//
//                if (lastSongId != null) {
//                    // Create a shell item. Media3 will automatically
//                    // pass this into onAddMediaItems() above to resolve it!
//                    val shellItem = MediaItem.Builder().setMediaId(lastSongId).build()
//
//                    future.set(
//                        MediaSession.MediaItemsWithStartPosition(
//                            listOf(shellItem),
//                            0,
//                            lastState.positionMs
//                        )
//                    )
//                } else {
//                    future.setException(Exception("No last state"))
//                }
//            }
//            return future
//        }


        // Note: You can also override onGetLibraryRoot and onGetChildren for Android Auto support
    }

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }


}
