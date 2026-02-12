package org.example.project.core.manager

import android.content.ComponentName
import android.content.Context
import android.util.Log
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.example.project.core.model.Song
import org.example.project.core.model.toSong
import org.example.project.core.repository.PlaybackRepository
import org.example.project.core.repository.YouTubeRepository
import org.example.project.core.service.MediaService


class MusicPlayerManagerImpl(
    private val context: Context,
    private val playbackRepository: PlaybackRepository,
    private val youTubeRepository: YouTubeRepository
) : MusicPlayerManager {

    private var controller: MediaController? = null

    // TODO: maybe these can move to its own state ?
    private val _isPlaying = MutableStateFlow(false)
    override val isPlaying = _isPlaying.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    override val currentPosition = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    override val duration = _duration.asStateFlow()

    private val _currentSong = MutableStateFlow<Song?>(null)
    override val currentSong = _currentSong.asStateFlow()

    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var positionUpdateJob: Job? = null

    private var isRestoringPlaybackState = false


    override fun initialise() {
        if (controller == null || controller?.isConnected == false) {
            initializeController()
        } else {
            // DOnt need to check if controller initialised because we check if media items == 0 inside restore playbackState
            restorePlaybackState()
        }
    }

    private fun initializeController() {
        val sessionToken = SessionToken(
            context,
            ComponentName(context, MediaService::class.java)
        )

        val controllerFuture = MediaController.Builder(context, sessionToken)
            .buildAsync()

        controllerFuture.addListener({
            controller = controllerFuture.get().apply {
                addListener(object : Player.Listener {
                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        _isPlaying.value = isPlaying
                        if (isPlaying) {
                            startPositionUpdates()
                        } else {
                            stopPositionUpdates()
                        }
                    }

                    override fun onPlaybackStateChanged(state: Int) {
                        when (state) {
                            Player.STATE_READY -> {
                                _duration.value = duration
                            }

                            Player.STATE_ENDED -> {
                                stopPositionUpdates()
                            }
                        }
                    }

                    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                        // If we arent restoring a saved state, we need to immediately update the state
                        // only need to update the song (title, image etc) and the duration to reset the bar to the start
                        if (!isRestoringPlaybackState) {
                            val song = mediaItem?.toSong()
                            _currentSong.value = song
                            _duration.value = 0L
                            // Save State
                            song?.let { coroutineScope.launch { playbackRepository.saveSong(song) } }
                        }
                    }
                })
            }
            restorePlaybackState()
        }, MoreExecutors.directExecutor())
    }

    private fun restorePlaybackState() {
        // Only if theres 0 items, we attempt to restore state. This can happen if we clear app, and restart, but this manager wasnt killed
        if (controller?.mediaItemCount == 0) {
            coroutineScope.launch {
                val lastState = playbackRepository.playbackState.first()
                val song = lastState.song
                val currentPosition = lastState.positionMs
                song?.let {
                    Log.d("Restoring state", "Found ${song.title} to restore")
                    isRestoringPlaybackState = true
                    prepare(song = song, autoPlay = false, startPosition = currentPosition)
                    _currentSong.value = song
                    _currentPosition.value = currentPosition
                    _duration.value = lastState.duration
                    isRestoringPlaybackState = false
                }
            }
        }
    }

    private fun startPositionUpdates() {
        positionUpdateJob?.cancel()

        positionUpdateJob = coroutineScope.launch {
            while (controller?.isPlaying == true) {
                _currentPosition.value = controller?.currentPosition ?: 0L
                delay(1000) // 200ms for smooth updates
            }
        }
    }

    private fun stopPositionUpdates() {
        positionUpdateJob?.cancel()
        positionUpdateJob = null
        _currentPosition.value = controller?.currentPosition ?: 0L
    }

    override suspend fun prepare(song: Song, autoPlay: Boolean, startPosition: Long?) {
        val metadata = MediaMetadata.Builder()
            .setTitle(song.title)
            .setArtist(song.artist)
            .setArtworkUri(song.thumbnailUrl?.toUri())
            .build()
        val mediaItem = MediaItem.Builder()
            .setMediaId(song.url)
            .setUri(song.url)
            .setMediaMetadata(metadata)
            .build()

        controller?.apply {
            setMediaItem(mediaItem, startPosition ?: 0L)
            prepare()
            playWhenReady = autoPlay
        }
    }

    override fun pause() {
        controller?.pause()
    }

    override fun play() {
        controller?.play()
    }

    override fun stop() {
        controller?.stop()
    }

    override fun seekTo(seconds: Long) {
        controller?.seekTo(seconds)
    }

}
