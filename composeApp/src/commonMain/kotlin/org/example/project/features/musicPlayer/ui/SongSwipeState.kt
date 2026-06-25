package org.example.project.features.musicPlayer.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.DraggableState
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import org.example.project.core.model.Song
import org.example.project.features.musicPlayer.model.PlayerQueue
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Swipe-to-change-song mechanic shared by [SongScreen]'s artwork and the [MusicPlayerBar].
 *
 * The current content slides out and fades while the adjacent song's content slides in from
 * the opposite edge. Releasing past the threshold finishes the slide, swaps the underlying
 * song, then snaps back to offset 0 — geometry already matches the new current song so there's
 * no visible jump. Releasing short of the threshold settles back to rest.
 *
 * [offsetPx] is mutated synchronously during the drag (never by a launched coroutine) so there's
 * no race with the release-time settle animation.
 */
@Stable
class SongSwipeState(
    private val maxDragPx: Float,
    private val swipeThresholdPx: Float,
) {
    internal var onNext: () -> Unit = {}
    internal var onPrevious: () -> Unit = {}

    /** Live drag offset of the current content, in pixels. */
    var offsetPx by mutableFloatStateOf(0f)
        private set

    /** Width of the swiped content, measured by [SongSwipeContent] so the preview docks at its edge. */
    var contentWidthPx by mutableFloatStateOf(0f)
        internal set

    private val settleAnimatable = Animatable(0f)
    private val settleSpec =
        spring<Float>(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)
    private val completeSpec = tween<Float>(durationMillis = 150)

    val draggableState = DraggableState { delta ->
        offsetPx = (offsetPx + delta).coerceIn(-maxDragPx, maxDragPx)
    }

    val progress: Float get() = (abs(offsetPx) / maxDragPx).coerceIn(0f, 1f)

    /** -1 while swiping toward "next", 1 while swiping toward "previous", 0 at rest. */
    val direction: Int
        get() = when {
            offsetPx < -0.5f -> -1
            offsetPx > 0.5f -> 1
            else -> 0
        }

    /**
     * Preview starts just past the content's own edge (fully hidden) and docks at 0 as drag
     * progress reaches 1, so it visibly travels in from the opposite side.
     */
    val previewOffsetPx: Float get() = -direction * contentWidthPx * (1f - progress)

    /** The song that previews in for the current swipe direction, or null at rest. */
    fun previewSong(queue: PlayerQueue): Song? = when (direction) {
        -1 -> queue.manual.firstOrNull() ?: queue.upcoming.firstOrNull()
        1 -> queue.history.lastOrNull()
        else -> null
    }

    suspend fun onDragStopped() {
        val dragged = offsetPx
        settleAnimatable.snapTo(dragged)
        if (abs(dragged) > swipeThresholdPx) {
            // Finish the slide in the direction it was already going (continues from the current
            // position, no jump) so the preview is fully docked before we swap the song.
            val target = if (dragged < 0) -maxDragPx else maxDragPx
            settleAnimatable.animateTo(target, completeSpec) { offsetPx = value }
            if (dragged < 0) onNext() else onPrevious()
            // Geometry already matches the new current song at offset 0.
            offsetPx = 0f
        } else {
            settleAnimatable.animateTo(0f, settleSpec) { offsetPx = value }
        }
    }
}

@Composable
fun rememberSongSwipeState(
    maxDrag: Dp,
    swipeThreshold: Dp,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
): SongSwipeState {
    val density = LocalDensity.current
    val maxDragPx = with(density) { maxDrag.toPx() }
    val swipeThresholdPx = with(density) { swipeThreshold.toPx() }
    val state = remember(maxDragPx, swipeThresholdPx) {
        SongSwipeState(maxDragPx, swipeThresholdPx)
    }
    // Refresh the callbacks each recomposition so they always target the latest viewModel.
    state.onNext = onNext
    state.onPrevious = onPrevious
    return state
}

/** Applies the horizontal swipe gesture backing [state]. */
fun Modifier.songSwipe(state: SongSwipeState): Modifier = draggable(
    orientation = Orientation.Horizontal,
    state = state.draggableState,
    onDragStopped = { state.onDragStopped() },
)

/**
 * Renders the swipe's two layers for [state] over [queue]: the incoming
 * [previewSong][SongSwipeState.previewSong] sliding in from the opposite edge, and the current song
 * ([queue].current) sliding out while fading. [layer] draws one song with the modifier it's handed
 * (which carries that layer's offset, plus alpha for the current one) — the bar passes a row, the
 * artwork passes a cover image. At rest only the current layer exists; nothing is drawn when the
 * queue has no current song.
 *
 * Measuring the content (for preview docking) and clipping the sliding layers are intrinsic to the
 * effect, so they're applied here — [modifier] only needs the caller's own layout/clip-shape (and
 * [songSwipe] if the gesture lives on this region). Keep [layer] to just the song's own visuals
 * plus the passed-in modifier.
 */
@Composable
fun SongSwipeContent(
    state: SongSwipeState,
    queue: PlayerQueue,
    modifier: Modifier = Modifier,
    layer: @Composable (song: Song, modifier: Modifier) -> Unit,
) {
    val current = queue.current ?: return
    Box(
        modifier = modifier
            .onSizeChanged { state.contentWidthPx = it.width.toFloat() }
            .clipToBounds()
    ) {
        state.previewSong(queue)?.let { preview ->
            layer(preview, Modifier.offset { IntOffset(state.previewOffsetPx.roundToInt(), 0) })
        }
        layer(
            current,
            Modifier
                .offset { IntOffset(state.offsetPx.roundToInt(), 0) }
                .alpha(1f - state.progress),
        )
    }
}
