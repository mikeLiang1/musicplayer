package org.example.project.core.manager

import android.content.ComponentName
import android.content.Context
import android.util.Log
import androidx.media3.common.MediaItem
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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.example.project.core.helper.toMediaItem
import org.example.project.core.model.PlayerState
import org.example.project.core.model.Song
import org.example.project.core.repository.SavedDataRepository
import org.example.project.core.service.MediaService
import org.schabi.newpipe.extractor.timeago.patterns.it

/**
 * Android implementation of MusicPlayerManager that wraps ExoPlayer.
 * Only handles playback - queue logic is managed by QueueManager.
 */
class MusicPlayerManagerImpl(
    private val context: Context,
    private val savedDataRepository: SavedDataRepository
) : MusicPlayerManager {

    private var controller: MediaController? = null

    private val _playerState = MutableStateFlow(PlayerState())
    override val playerState = _playerState.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    override val currentPosition = _currentPosition.asStateFlow()

    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var positionUpdateJob: Job? = null

    private var isAppInForeground = true

    private var hasRestoredPosition = false

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
                        if (isPlaying && isAppInForeground) {
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
                            Player.STATE_BUFFERING -> {
                                // Show loading state if needed
                            }
                            Player.STATE_READY -> {
                                // Update duration when ready
                                _playerState.update { it.copy(durationMs = controller?.duration ?: 0L) }
                            }
                        }
                    }

                    override fun onPositionDiscontinuity(
                        oldPosition: Player.PositionInfo,
                        newPosition: Player.PositionInfo,
                        reason: Int
                    ) {
                        // Update position on seek
                        _currentPosition.value = newPosition.positionMs
                    }
                })
            }
        }, MoreExecutors.directExecutor())
    }

    private fun restorePlaybackState() {
        // Only if theres 0 items, we attempt to restore state. This can happen if we clear app, and restart, but this manager wasnt killed
        Log.d("Logging", "restoring playback items = ${controller?.mediaItemCount}")
        if (controller?.mediaItemCount == 0) {
            coroutineScope.launch {
                val lastState = savedDataRepository.getPlaybackState()
                lastState?.let {
                    val queue = lastState.queue
                    val song = lastState.queue.find { song ->
                        song.uniqueId == lastState.currentSongId
                    }
                    val currentPosition = lastState.positionMs
                    val index = lastState.index ?: 0
                    song?.let {
                        setPlaylist(
                            queue,
                            autoPlay = false,
                            positionMs = currentPosition,
                            startIndex = index
                        )
                        _currentPosition.value = currentPosition
                    }
                }
            }
        }
    }

    override fun setPlaylist(songs: List<Song>, startIndex: Int, positionMs: Long, autoPlay: Boolean) {
        val mediaItems = songs.map { it.toMediaItem() }
        _playerState.update { it.copy(currentIndex = 0, currentSong = songs[startIndex]) }
        controller?.apply {
            setMediaItems(mediaItems, startIndex, positionMs)
            prepare()
            playWhenReady = autoPlay
        }
        Log.d("logging", "set playlist")
    }

    override fun replaceFullQueueKeepingCurrentSong(songs: List<Song>, newIndex: Int) {
        val controller = controller ?: return
        val originalCurrentIndex = controller.currentMediaItemIndex  // capture before any changes
        Log.d("logging", "replacequeue running")

        // Check if we're changing to a different song
        val currentSongInController = controller.currentMediaItem
        val newSong = songs.getOrNull(newIndex)?.toMediaItem()
        val isChangingSong = currentSongInController == null || newSong == null ||
                             currentSongInController.mediaId != newSong.mediaId

        if (isChangingSong) {
            // We're changing songs, so we should rebuild the entire queue
            val mediaItems = songs.map { it.toMediaItem() }
            val wasPlaying = controller.isPlaying
            controller.setMediaItems(mediaItems, newIndex, 0L)
            _playerState.update { it.copy(currentIndex = newIndex, currentSong = songs.getOrNull(newIndex)) }
            return
        }

        // Original logic for when current song stays the same
        // Replace after first (indices unaffected)
        val upcoming = songs.drop(newIndex + 1)
        controller.removeMediaItems(originalCurrentIndex + 1, controller.mediaItemCount)
        controller.addMediaItems(originalCurrentIndex + 1, upcoming.map { it.toMediaItem() })

        // Replace before (shifts current index but current item unaffected)
        val played = songs.subList(0, newIndex)
        controller.removeMediaItems(0, originalCurrentIndex)
        controller.addMediaItems(0, played.map { it.toMediaItem() })

        _playerState.update { it.copy(currentIndex = newIndex) }
    }



    override fun play() {
        controller?.play()
    }

    override fun pause() {
        controller?.pause()
    }

    override fun stop() {
        controller?.stop()
    }

    override fun seekTo(positionMs: Long) {
        controller?.seekTo(positionMs)
        _currentPosition.value = positionMs
    }

    override fun release() {
        controller?.release()
        controller = null
        stopPositionUpdates()
    }

    override fun onAppEnteredForeground() {
        isAppInForeground = true
        if (controller?.isPlaying == true) {
            startPositionUpdates()
        }
    }

    override fun onAppEnteredBackground() {
        isAppInForeground = false
        stopPositionUpdates()
    }

    private fun startPositionUpdates() {
        stopPositionUpdates()
        positionUpdateJob = coroutineScope.launch {
            while (true) {
                controller?.currentPosition?.let { position ->
                    _currentPosition.value = position
                }
                delay(1000) // Update every second
            }
        }
    }

    private fun stopPositionUpdates() {
        positionUpdateJob?.cancel()
        positionUpdateJob = null
    }

}
