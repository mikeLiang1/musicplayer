package org.example.project.core.manager

import android.content.ComponentName
import android.content.Context
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Player.DISCONTINUITY_REASON_SEEK
import androidx.media3.common.Player.TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED
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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.example.project.core.helper.toMediaItem
import org.example.project.core.helper.toSong
import org.example.project.core.model.PlayerState
import org.example.project.core.model.Song
import org.example.project.core.repository.QueueRepository
import org.example.project.core.repository.SavedDataRepository
import org.example.project.core.service.MediaService


class MusicPlayerManagerImpl(
    private val context: Context,
    private val repo: SavedDataRepository,
    private val queueRepository: QueueRepository
) : MusicPlayerManager {
    private var controller: MediaController? = null

    private val _playerState = MutableStateFlow(PlayerState())
    override val playerState = _playerState.asStateFlow()
    private val _currentPosition = MutableStateFlow(0L)
    override val currentPosition = _currentPosition.asStateFlow()

    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var positionUpdateJob: Job? = null

    private var queueSaveJob: Job? = null

    private var isAppInForeground = true


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

                            // Maybe TODO: Use buffering state to show loading
                            Player.STATE_BUFFERING -> {
                                Log.d("logging", "bufferoing")
                            }

                            Player.STATE_READY -> {
                                Log.d("logging", "ready")
                            }
                        }
                    }

                    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                        // If we arent restoring a saved state, we need to immediately update the state
                        // only need to update the song (title, image etc) and force current position to start and index update index
                        val song = mediaItem?.toSong()
                        val index = currentMediaItemIndex
                        _playerState.update {
                            it.copy(
                                currentSong = song,
                                currentIndex = index
                            )
                        }
                        // Save State
                        song?.let {
                            ioScope.launch {
                                repo.saveCurrentSongIdAndIndex(
                                    song.uniqueId,
                                    index
                                )
                            }
                        }

                    }

                    // TODO: Remove
                    override fun onTimelineChanged(timeline: Timeline, reason: Int) {
                        // Only if queue order / items change
                        if (reason == TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED) {

                            val items = buildList {
                                for (i in 0 until (mediaItemCount)) {
                                    add(getMediaItemAt(i).toSong())
                                }
                            }

                            ioScope.launch {
                                queueSaveJob?.cancel()
                                queueSaveJob = launch {
                                    delay(2000)
                                    if (items.isNotEmpty()) {
                                        repo.saveQueue(items)
                                    }
                                }
                            }

                        }
                    }

                    override fun onPositionDiscontinuity(
                        oldPosition: Player.PositionInfo,
                        newPosition: Player.PositionInfo,
                        reason: Int
                    ) {
                        // This helps still update position when paused and is not tracking
                        if (reason == DISCONTINUITY_REASON_SEEK) {
                            _currentPosition.value = newPosition.positionMs

                        }
                    }

                })
            }
            restorePlaybackState()
        }, MoreExecutors.directExecutor())
    }

    // TODO: i would rather have this in playback repositroy init
    private fun restorePlaybackState() {
        // Only if theres 0 items, we attempt to restore state. This can happen if we clear app, and restart, but this manager wasnt killed
        Log.d("Logging", "restoring playback items = ${controller?.mediaItemCount}")
        if (controller?.mediaItemCount == 0) {
            coroutineScope.launch {
                val lastState = repo.getPlaybackState()
                lastState?.let {
                    val queue = lastState.queue
                    val song = lastState.queue.find { song ->
                        song.uniqueId == lastState.currentSongId
                    }
                    val currentPosition = lastState.positionMs
                    val index = lastState.index ?: 0
                    song?.let {
                        setQueue(
                            queue,
                            autoPlay = false,
                            startPosition = currentPosition,
                            startIndex = index
                        )
                        queueRepository.setQueue(
                            queue,
                            lastState.isShuffled,
                            lastState.originalQueue
                        )
                        _currentPosition.value = currentPosition
                    }
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
    override suspend fun setQueue(
        songs: List<Song>,
        autoPlay: Boolean,
        startPosition: Long?,
        startIndex: Int
    ) {
        val mediaItems = songs.map { it.toMediaItem() }

        controller?.apply {
            setMediaItems(mediaItems, startIndex, startPosition ?: 0L)
            prepare()
            playWhenReady = autoPlay
        }
    }


    // Replace before and af
    override fun replaceFullQueueKeepingCurrentSong(songs: List<Song>, newIndex: Int) {
        val controller = controller ?: return
        val originalCurrentIndex = controller.currentMediaItemIndex  // capture before any changes

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
        controller?.play()
    }

    override fun skipToPrevious() {
        controller?.seekToPrevious()
    }

    override fun seekToIndex(index: Int) {
        controller?.seekToDefaultPosition(index)
        controller?.play()
    }


    override fun shuffleOn() {
        controller?.shuffleModeEnabled = true
    }

    override fun shuffleOff() {
        controller?.shuffleModeEnabled = false
    }

    override fun onAppEnteredForeground() {
        Log.d("Logging", "app entered foregiroynd")
        isAppInForeground = true
        if (controller?.isPlaying == true) startPositionUpdates()
    }

    override fun onAppEnteredBackground() {
        isAppInForeground = false
        Log.d("Logging", "app entered background")
        stopPositionUpdates()
    }

    private fun startPositionUpdates() {
        positionUpdateJob?.cancel()

        positionUpdateJob = coroutineScope.launch {
            while (controller?.isPlaying == true && isAppInForeground) {
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
