package org.example.project.core.manager

import android.content.ComponentName
import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Timeline
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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.example.project.core.helper.toMediaItem
import org.example.project.core.helper.toSong
import org.example.project.core.model.PlayerState
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

    private val _playerState = MutableStateFlow(PlayerState())
    override val playerState = _playerState.asStateFlow()
    private val _currentPosition = MutableStateFlow(0L)
    override val currentPosition = _currentPosition.asStateFlow()

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
                        _playerState.update { it.copy(isPlaying = isPlaying) }
                        if (isPlaying) {
                            startPositionUpdates()
                        } else {
                            stopPositionUpdates()
                        }
                    }

                    override fun onPlaybackStateChanged(state: Int) {
                        when (state) {

                            Player.STATE_ENDED -> {
                                stopPositionUpdates()
                            }
                        }
                    }

                    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                        // If we arent restoring a saved state, we need to immediately update the state
                        // only need to update the song (title, image etc) and force current position to start and index update index
                        if (!isRestoringPlaybackState) {
                            val song = mediaItem?.toSong()
                            _playerState.update {
                                it.copy(
                                    currentSong = song,
                                    currentIndex = controller?.currentMediaItemIndex ?: 0
                                )
                            }
                            // Save State
                            song?.let { coroutineScope.launch { playbackRepository.saveSong(song) } }
                        }
                    }

                    override fun onTimelineChanged(timeline: Timeline, reason: Int) {
                        val items = buildList {
                            for (i in 0 until (controller?.mediaItemCount ?: 0)) {
                                controller?.getMediaItemAt(i)?.toSong()?.let { add(it) }
                            }
                        }

                        _playerState.update {
                            it.copy(queue = items)
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
                    isRestoringPlaybackState = true
                    val relatedSongs = youTubeRepository.getPlaylistRadio(song.url)
                    setQueue(relatedSongs, autoPlay = false, startPosition = currentPosition)
                    _playerState.update { it.copy(currentSong = song) }
                    _currentPosition.value = currentPosition
                    isRestoringPlaybackState = false
                }
            }
        }
    }


    override suspend fun prepare(song: Song, autoPlay: Boolean, startPosition: Long?) {
        val mediaItem = song.toMediaItem()

        controller?.apply {
            setMediaItem(mediaItem, startPosition ?: 0L)
            prepare()
            playWhenReady = autoPlay
        }
    }

    // TODO: If we are playing a playilist need to save the index and set index
    override suspend fun setQueue(songs: List<Song>, autoPlay: Boolean, startPosition: Long?) {
        val mediaItems = songs.map { it.toMediaItem() }

        controller?.apply {
            setMediaItems(mediaItems, 0, startPosition ?: 0L)
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

    override fun skipToNext() {
        controller?.seekToNext()
    }

    override fun skipToPrevious() {
        controller?.seekToPrevious()
    }

    override fun seekToIndex(index: Int) {
        controller?.seekToDefaultPosition(index)
        controller?.play()
    }

    private fun startPositionUpdates() {
        positionUpdateJob?.cancel()

        positionUpdateJob = coroutineScope.launch {
            while (controller?.isPlaying == true) {
                _currentPosition.value = controller?.currentPosition ?: 0L
                delay(1000)
            }
        }
    }

    private fun stopPositionUpdates() {
        positionUpdateJob?.cancel()
        positionUpdateJob = null
        _currentPosition.value = controller?.currentPosition ?: 0L
    }

}
