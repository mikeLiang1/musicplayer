package org.example.project.features.musicPlayer.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.launchIn
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

@OptIn(FlowPreview::class)
class MusicPlayerViewModel(
    private val musicPlayerManager: MusicPlayerManager,
    private val queueManager: QueueManager,
    private val playbackRepository: PlaybackRepository
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
        // While buffering, a tap always means "play when ready" — never pause.
        if (playerState.value.isBuffering) {
            musicPlayerManager.play()
            return
        }
        if (playerState.value.isPlaying) musicPlayerManager.pause()
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
    val playbackMode: PlaybackMode = PlaybackMode.OFF
)

sealed interface MusicPlayerEffect {
    data object ScrollUpWhenHistoryOpened : MusicPlayerEffect
    data object ScrollToFirst : MusicPlayerEffect
}
