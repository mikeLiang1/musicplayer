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
import org.example.project.core.model.FlowMode
import org.example.project.core.model.PlayerState
import org.example.project.core.model.Song
import org.example.project.core.repository.SavedDataRepository
import org.example.project.core.service.MediaService
import org.example.project.features.musicPlayer.ui.BucketCrossing
import org.schabi.newpipe.extractor.timeago.patterns.it
import java.util.UUID


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
                        val lastPlayedIndex = _playerState.value.currentIndex

                        if (_playerState.value.currentSong?.isManual == true) {
                            controller?.removeMediaItem(lastPlayedIndex)
                        }
                        val newIndex = currentMediaItemIndex
                        // If user has selected place in queue
                        if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_SEEK) {
                            val manualCount = _playerState.value.manualItemCount
                            val firstManualIndex = _playerState.value.firstManualIndex
                            if (manualCount > 0 && firstManualIndex != null && firstManualIndex != newIndex) {

                                val adjustedIndex = if (newIndex > firstManualIndex) {
                                    newIndex - manualCount + 1
                                } else {
                                    newIndex + 1
                                }

                                controller?.moveMediaItems(
                                    firstManualIndex,
                                    firstManualIndex + manualCount,
                                    adjustedIndex
                                )
                            }

                        }

                        val song = mediaItem?.toSong()


                        _playerState.update {
                            it.copy(
                                currentSong = song,
                                currentIndex = newIndex
                            )
                        }

                        // Save State
                        song?.let {
                            ioScope.launch {
                                savedDataRepository.saveCurrentSongIdAndIndex(
                                    song.uniqueId,
                                    newIndex
                                )
                            }
                        }
                    }

                    override fun onTimelineChanged(timeline: Timeline, reason: Int) {
                        // Only if queue order / items change
                        if (reason == TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED) {
                            var manualItemCount = 0
                            var firstManualIndex: Int? = null

                            val items = buildList {
                                for (i in 0 until (mediaItemCount)) {
                                    val song = getMediaItemAt(i).toSong()
                                    add(song)
                                    if (song.isManual) {
                                        if (firstManualIndex == null) firstManualIndex = i
                                        manualItemCount++
                                    }
                                }
                            }

                            _playerState.update {
                                it.copy(
                                    queue = items,
                                    manualItemCount = manualItemCount,
                                    firstManualIndex = firstManualIndex
                                )
                            }

                            ioScope.launch {
                                queueSaveJob?.cancel()
                                queueSaveJob = launch {
                                    delay(2000)
                                    if (items.isNotEmpty()) {
                                        savedDataRepository.saveQueue(items)
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
                val lastState = savedDataRepository.getPlaybackState()
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


    override fun shuffle() {
        val controller = controller ?: return
        val currentState = _playerState.value
        val currentIndex = currentState.currentIndex
        val queue = currentState.queue
        val queueLength = queue.size
        val isShuffled = currentState.isShuffled

        if (!isShuffled) {
            // --- SHUFFLE ON ---
            // Save the current state as the original reference
            _playerState.update { it.copy(originalQueue = queue, isShuffled = true) }

            // Partition items AFTER the current index
            val (manual, upcoming) = (currentIndex + 1 until queueLength)
                .map { queue[it] }
                .partition { it.isManual }

            val shuffled = upcoming.shuffled()

            // Surgical Update: Remove everything after current song
            controller.removeMediaItems(currentIndex + 1, controller.mediaItemCount)
            // Add back: Manual (Play Next) items first, then the shuffled remaining items
            controller.addMediaItems(
                currentIndex + 1,
                (manual + shuffled).map { it.toMediaItem() }
            )

            ioScope.launch {
                savedDataRepository.saveOriginalQueue(queue)
                savedDataRepository.saveIsShuffled(true)
            }
        } else {
            // --- SHUFFLE OFF (UNSHUFFLE) ---
            val originalQueue = currentState.originalQueue
            val currentSongId = currentState.currentSong?.uniqueId ?: return
            val newIndexInOriginal = originalQueue.indexOfFirst { it.uniqueId == currentSongId }

            if (newIndexInOriginal == -1) return

            // 1. Identify Manual items currently in the upcoming queue (after current index)
            val currentManualSongs = queue.subList(currentIndex + 1, queueLength).filter { it.isManual }
            val manualIds = currentManualSongs.map { it.uniqueId }.toSet()

            // 2. Prepare restored components, filtering out manual IDs to avoid duplicates
            // We remove currentManualSongs from the original sequence because they'll be
            // re-inserted immediately after the current song.
            val playedRestored = originalQueue.subList(0, newIndexInOriginal)
                .filter { it.uniqueId !in manualIds }

            val upcomingRestored = originalQueue.subList(newIndexInOriginal + 1, originalQueue.size)
                .filter { it.uniqueId !in manualIds && !it.isManual }

            // 3. Surgical Queue Restoration in MediaController
            // Step A: Remove everything before the current song
            controller.removeMediaItems(0, currentIndex)

            // Step B: Insert the restored history (prefix)
            // Now the current song is shifted to index: playedRestored.size
            controller.addMediaItems(0, playedRestored.map { it.toMediaItem() })

            val newCurrentIndex = playedRestored.size

            // Step C: Clear everything after the current song and add upcoming items
            controller.removeMediaItems(newCurrentIndex + 1, controller.mediaItemCount)
            controller.addMediaItems(
                newCurrentIndex + 1,
                (currentManualSongs + upcomingRestored).map { it.toMediaItem() }
            )

            // 4. Update state
            _playerState.update { it.copy(currentIndex = newCurrentIndex, isShuffled = false) }
            ioScope.launch {
                savedDataRepository.saveIndex(index = newCurrentIndex)
                savedDataRepository.saveIsShuffled(false)
            }
        }
    }


    override fun addToQueue(song: Song) {
        val insertIndex = (controller?.currentMediaItemIndex ?: return) + 1
        controller?.addMediaItem(
            insertIndex,
            song.copy(uniqueId = UUID.randomUUID().toString(), isManual = true).toMediaItem()
        )
    }

    override fun moveSong(queue: List<Song>) {
        val currentIndex = _playerState.value.currentIndex
        controller?.removeMediaItems(currentIndex + 1, controller?.mediaItemCount ?: return)
        controller?.addMediaItems(
            currentIndex + 1,
            queue.drop(currentIndex + 1).map { it.toMediaItem() }
        )
    }

    override fun cycleFlowMode() {
        val next = when (_playerState.value.flowMode) {
            FlowMode.STOP_AT_END -> FlowMode.REPEAT_ALL
            FlowMode.REPEAT_ALL -> FlowMode.INFINITE
            FlowMode.INFINITE -> FlowMode.STOP_AT_END
        }

        controller?.repeatMode = next.toMedia3RepeatMode()

        _playerState.update { it.copy(flowMode = next) }

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
                // update every second
                delay(1000)
            }
        }
    }

    private fun stopPositionUpdates() {
        positionUpdateJob?.cancel()
        positionUpdateJob = null
        _currentPosition.value = controller?.currentPosition ?: 0L
    }

    private fun FlowMode.toMedia3RepeatMode() = when (this) {
        FlowMode.STOP_AT_END -> Player.REPEAT_MODE_OFF
        FlowMode.REPEAT_ALL -> Player.REPEAT_MODE_ALL
        FlowMode.INFINITE -> Player.REPEAT_MODE_OFF // handled manually
    }
}
