package org.example.project.core.manager

import android.content.ComponentName
import android.content.Context
import android.util.Log
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
import org.example.project.core.service.MediaService

/**
 * Android implementation of MusicPlayerManager that wraps ExoPlayer.
 * Only handles playback - queue logic is managed by QueueManager.
 */
class MusicPlayerManagerImpl(
    private val context: Context
) : MusicPlayerManager {

    private var controller: MediaController? = null

    private val _playerState = MutableStateFlow(PlayerState())
    override val playerState = _playerState.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    override val currentPosition = _currentPosition.asStateFlow()

    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var positionUpdateJob: Job? = null

    private var isAppInForeground = true


    override fun initialise() {
        if (controller == null || controller?.isConnected == false) {
            initializeController()
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
                            Player.STATE_BUFFERING -> {
                                _playerState.update { it.copy(isBuffering = true) }
                            }

                            Player.STATE_READY -> {
                                // Update duration when ready
                                _playerState.update {
                                    it.copy(
                                        durationMs = controller?.duration ?: 0L,
                                        isBuffering = false
                                    )
                                }
                            }
                        }
                    }
                })
            }
        }, MoreExecutors.directExecutor())
    }

    // Queue restoration is now handled by QueueManager via ViewModel
    // This method is kept for potential position restoration in the future
    override fun setPlaylist(songs: List<Song>, startIndex: Int, positionMs: Long, autoPlay: Boolean) {
        val mediaItems = songs.map { it.toMediaItem() }
        controller?.apply {
            playWhenReady = if (isPlaying) true else autoPlay
            setMediaItems(mediaItems, startIndex, positionMs)
            prepare()
        }
        _currentPosition.value = positionMs
        Log.d("logging", "set playlist with position ${_currentPosition.value}")
    }

    override fun replaceFullQueueKeepingCurrentSong(songs: List<Song>, newIndex: Int) {
        val controller = controller ?: return
        val originalCurrentIndex = controller.currentMediaItemIndex  // capture before any changes
        Log.d("logging", "replacequeue running")

        // Original logic for when current song stays the same
        // Replace after first (indices unaffected)
        val upcoming = songs.drop(newIndex + 1)
        controller.removeMediaItems(originalCurrentIndex + 1, controller.mediaItemCount)
        controller.addMediaItems(originalCurrentIndex + 1, upcoming.map { it.toMediaItem() })

        // Replace before (shifts current index but current item unaffected)
        val played = songs.subList(0, newIndex)
        controller.removeMediaItems(0, originalCurrentIndex)
        controller.addMediaItems(0, played.map { it.toMediaItem() })
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

    override fun seekToDefaultPosition(index: Int) {
        controller?.seekToDefaultPosition(index)
    }

    private fun startPositionUpdates() {
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
