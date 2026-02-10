package org.example.project.core.manager

import android.content.ComponentName
import android.content.Context
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
import org.example.project.core.repository.PlaybackRepository
import org.example.project.core.repository.YouTubeRepository
import org.example.project.core.service.MediaService


class MusicPlayerManagerImpl(
    private val context: Context,
    private val playbackRepository: PlaybackRepository,
    private val youTubeRepository: YouTubeRepository
) : MusicPlayerManager {

    private var controller: MediaController? = null

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

    init {
        initializeController()
        grabLastState()
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
                                _currentPosition.value = duration // Set to end
                            }
                        }
                    }

                    override fun onPositionDiscontinuity(
                        oldPosition: Player.PositionInfo,
                        newPosition: Player.PositionInfo,
                        reason: Int
                    ) {
                        _currentPosition.value = currentPosition
                    }
                })
            }
        }, MoreExecutors.directExecutor())
    }

    private fun grabLastState() {
        coroutineScope.launch {
            val lastState = playbackRepository.playbackState.first() // Get only the first emission
            val song = lastState.song
            val currentPosition = lastState.positionMs
            _currentSong.value = song
            _currentPosition.value = currentPosition
            song?.let {
                prepare(song = song, autoPlay = false, startPosition = currentPosition)
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

        val streamUrl = youTubeRepository.getStreamUrl(song.url) ?: throw IllegalStateException("Failed to get stream URL")
        _currentSong.value = song

            playbackRepository.saveSong(song)


        val metadata = MediaMetadata.Builder()
            .setTitle(song.title)
            .setArtist(song.artist)
            .setArtworkUri(song.thumbnailUrl?.toUri())
            .build()

        val mediaItem = MediaItem.Builder()
            .setMediaId(streamUrl)
            .setUri(streamUrl)
            .setMediaMetadata(metadata)
            .build()

        // Use the controller (which controls the player in the service)
        controller?.apply {
            setMediaItem(mediaItem)
            prepare()
            if (startPosition != null) {
                seekTo(startPosition)
            }
            if (autoPlay) {
                play()
            }
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

    override fun getCurrentPosition(): Long? {
        TODO("Not yet implemented")
    }

    override fun getDuration(): Long? {
        TODO("Not yet implemented")
    }

    override fun seekTo(seconds: Long) {
        TODO("Not yet implemented")
    }


    override fun release() {
        TODO("Not yet implemented")
    }

}
