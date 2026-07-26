package org.example.project.features.musicPlayer.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.example.project.core.manager.MusicPlayerManager
import org.example.project.core.manager.PlaybackMode
import org.example.project.core.manager.QueueIntent
import org.example.project.core.manager.QueueManager
import org.example.project.core.manager.QueueState
import org.example.project.core.repository.PlaybackRepository
import org.example.project.features.musicPlayer.model.PlayerQueue
import org.example.project.features.playlist.repository.PlaylistRepository
import kotlin.time.Clock

@OptIn(FlowPreview::class)
class MusicPlayerViewModel(
    private val musicPlayerManager: MusicPlayerManager,
    private val queueManager: QueueManager,
    private val playbackRepository: PlaybackRepository,
    private val playlistRepository: PlaylistRepository
) : ViewModel() {

    // Owned, mutable UI bits (fullscreen, history pill, queue editing).
    private val _uiState = MutableStateFlow(MusicPlayerUiState())
    private val _effect = MutableSharedFlow<MusicPlayerEffect>()
    val effect: SharedFlow<MusicPlayerEffect> = _effect.asSharedFlow()
    val playerState = musicPlayerManager.playerState
    val currentPosition = musicPlayerManager.currentPosition

    val uiState: StateFlow<MusicPlayerUiState> =
        combine(_uiState, queueManager.queueState) { ui, qs ->
            ui.copy(isShuffled = qs.isShuffled, playbackMode = qs.playbackMode)
        }.stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            _uiState.value.copy(
                isShuffled = queueManager.queueState.value.isShuffled,
                playbackMode = queueManager.queueState.value.playbackMode
            )
        )

    // What the UI consumes — editing snapshot if active, else live projection.
    // Kept as its own flow (not in uiState) so the always-visible mini-bar and the
    // now-playing page don't recompose on shuffle/repeat/edit toggles.
    val displayQueue: StateFlow<PlayerQueue> =
        combine(_uiState, queueManager.queueState) { ui, qs ->
            ui.editingQueue ?: qs.toPlayerQueue()
        }.stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            queueManager.queueState.value.toPlayerQueue()
        )

    // Liked state for the now-playing song, observed from the DB so it stays right when the
    // song is liked/unliked elsewhere (song menu, add-to-playlist sheet, Liked Songs screen).
    @OptIn(ExperimentalCoroutinesApi::class)
    val isCurrentSongLiked: StateFlow<Boolean> = queueManager.queueState
        .map { it.current?.url }
        .distinctUntilChanged()
        .flatMapLatest { url ->
            if (url == null) flowOf(false) else playlistRepository.observeIsSongLiked(url)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    init {
        // Attach the intent observer BEFORE restore so the NewQueue intent emitted during
        // restoration reaches a live collector instead of relying on channel buffering.
        // Sync queue changes to music player (for future changes)
        queueManager.intent
            .onEach { intent ->
                val state = queueManager.queueState.value
                val queue = state.playbackQueue

                if (queue.isEmpty()) return@onEach

                when (intent) {
                    is QueueIntent.ReplaceQueue ->
                        musicPlayerManager.replaceFullQueueKeepingCurrentSong(
                            queue,
                            intent.newIndex
                        )

                    is QueueIntent.SeekToItem -> {
                        musicPlayerManager.seekToDefaultPosition(intent.newIndex)
                    }

                    is QueueIntent.SeekAndRebuild -> {
                        musicPlayerManager.seekToDefaultPosition(intent.mediaIndex)
                        musicPlayerManager.replaceFullQueueKeepingCurrentSong(
                            queue,
                            intent.queueIndex
                        )
                    }

                    is QueueIntent.NewQueue ->
                        musicPlayerManager.setPlaylist(
                            state.baseQueue,
                            state.playbackCurrentIndex,
                            intent.positionMs,
                            state.autoPlay
                        )
                }
            }
            .launchIn(viewModelScope)

        viewModelScope.launch {
            restorePlaybackState()

            // Debounced save of queue state — attached AFTER restore so we never persist the
            // pre-restore empty state over good saved data.
            queueManager.queueState
                .debounce(500)
                .onEach { state -> playbackRepository.saveQueueState(state) }
                .launchIn(viewModelScope)
        }
    }

    private suspend fun restorePlaybackState() {
        // Restore queue + position in a single DB read.
        val restored = playbackRepository.getRestoredPlayback()
        restored?.let { (state, positionMs) ->
            queueManager.restoreState(state, positionMs)
        }
    }

    // ── Playback ──────────────────────────────────────
    fun onPlayPauseClicked() {
        val state = playerState.value
        // While buffering, a tap toggles the play-when-ready intent: arm it (will play
        // once loaded) or disarm it (will start paused). Drives the ▶-in-spinner cue.
        if (state.isBuffering) {
            if (state.playWhenReady) musicPlayerManager.pause()
            else musicPlayerManager.play()
            return
        }
        if (state.isPlaying) musicPlayerManager.pause()
        else musicPlayerManager.play()
    }

    fun onNextClicked() = queueManager.playNext()
    fun onPreviousClicked() = queueManager.playPrevious()
    fun onSeekTo(seconds: Long) = musicPlayerManager.seekTo(seconds)

    fun changeShuffleOption() {
        val state = queueManager.queueState.value
        if (state.isShuffled) {
            queueManager.unshuffle()
        } else {
            queueManager.shuffle()
        }
    }

    fun togglePlaybackMode() = queueManager.togglePlaybackMode()

    // ── Like ──────────────────────────────────────────
    /** Idempotent: only ever adds. Unliking happens from the add-to-playlist sheet. */
    fun likeCurrentSong() {
        val song = queueManager.queueState.value.current ?: return
        viewModelScope.launch {
            playlistRepository.likeSong(song)
        }
    }

    // ── Sleep Timer ───────────────────────────────────
    private var sleepTimerJob: Job? = null

    fun setSleepTimer(minutes: Int) {
        sleepTimerJob?.cancel()
        val durationMs = minutes * 60_000L
        val endAt = Clock.System.now().toEpochMilliseconds() + durationMs
        _uiState.update { it.copy(sleepTimerEndAtMs = endAt, sleepTimerEndOfTrack = false) }
        sleepTimerJob = viewModelScope.launch {
            delay(durationMs)
            musicPlayerManager.pause()
            _uiState.update { it.copy(sleepTimerEndAtMs = null) }
        }
    }

    fun setSleepTimerEndOfTrack() {
        sleepTimerJob?.cancel()
        _uiState.update { it.copy(sleepTimerEndAtMs = null, sleepTimerEndOfTrack = true) }
        sleepTimerJob = viewModelScope.launch {
            // Wait for the current song to change (i.e. the track it was set on finishes), then pause.
            queueManager.queueState
                .map { it.current?.uniqueId }
                .distinctUntilChanged()
                .drop(1)
                .first()
            musicPlayerManager.pause()
            _uiState.update { it.copy(sleepTimerEndOfTrack = false) }
        }
    }

    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        _uiState.update { it.copy(sleepTimerEndAtMs = null, sleepTimerEndOfTrack = false) }
    }

    // ── UI State ──────────────────────────────────────
    fun setFullScreen(fullScreen: Boolean) {
        _uiState.update { it.copy(isFullScreenVisible = fullScreen) }
    }

    fun onHistoryPillClicked() {
        viewModelScope.launch {
            if (uiState.value.showHistory) {
                _uiState.update { it.copy(showHistory = false) }
                _effect.emit(MusicPlayerEffect.ScrollToFirst)
            } else {
                _uiState.update { it.copy(showHistory = true) }
                _effect.emit(MusicPlayerEffect.ScrollUpWhenHistoryOpened)
            }
        }
    }

    fun changePlayingToSong(songId: String) {
        queueManager.playSongFromQueue(songId)
    }


    // ── Queue Edit ────────────────────────────────────
    fun onEditQueueClicked() {
        _uiState.update {
            it.copy(
                editingQueue = if (it.editingQueue != null) null else queueManager.queueState.value.toPlayerQueue(),
                isEditingQueue = !_uiState.value.isEditingQueue
            )
        }
    }

    fun onMove(fromKey: String, toKey: String) {
        val current = _uiState.value.editingQueue ?: return
        val combined = (current.manual + current.upcoming).toMutableList()
        val manualCount = current.manual.size

        val fromIndex =
            combined.indexOfFirst { it.uniqueId == fromKey }.takeIf { it != -1 } ?: return

        val toIndex = when (toKey) {
            "header_manual" -> 0
            "header_upcoming" -> manualCount  // drop on Queue header = start of upcoming
            "footer_repeat_mode" -> combined.size
            else -> combined.indexOfFirst { it.uniqueId == toKey }.takeIf { it != -1 } ?: return
        }

        combined.add(toIndex, combined.removeAt(fromIndex))

        val newManualCount = when {
            fromIndex < manualCount && toIndex >= manualCount -> manualCount - 1
            fromIndex >= manualCount && toIndex < manualCount -> manualCount + 1
            else -> manualCount
        }

        _uiState.update {
            it.copy(
                editingQueue = current.copy(
                    manual = combined.take(newManualCount),
                    upcoming = combined.drop(newManualCount)
                )
            )
        }
    }

    fun onDragEnd() {
        val editing = _uiState.value.editingQueue ?: return
        queueManager.replaceQueuesPreservingState(
            baseQueue = editing.history + listOfNotNull(editing.current) + editing.upcoming,
            manualQueue = editing.manual,
            currentBaseIndex = editing.history.size
        )
    }

    // ── Queue Management ──────────────────────────────

    fun removeSong(songId: String) {
        queueManager.removeSong(uniqueId = songId)
    }

    // Single place that re-shapes QueueManager's QueueState into the UI-facing PlayerQueue.
    private fun QueueState.toPlayerQueue() = PlayerQueue(
        history = history,
        current = current,
        manual = manualUpNext,
        upcoming = normalUpNext
    )

}

data class MusicPlayerUiState(
    val isFullScreenVisible: Boolean = false,
    val showHistory: Boolean = false,
    val isEditingQueue: Boolean = false,
    val editingQueue: PlayerQueue? = null,
    // Derived from QueueManager; populated by the uiState combine, not the mutators.
    val isShuffled: Boolean = false,
    val playbackMode: PlaybackMode = PlaybackMode.OFF,
    // Sleep timer: either a target timestamp (duration mode) or an end-of-track flag, never both.
    val sleepTimerEndAtMs: Long? = null,
    val sleepTimerEndOfTrack: Boolean = false
)

sealed interface MusicPlayerEffect {
    data object ScrollUpWhenHistoryOpened : MusicPlayerEffect
    data object ScrollToFirst : MusicPlayerEffect
}
