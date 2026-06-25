package org.example.project.features.musicPlayer.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import kotlinx.coroutines.launch
import org.example.project.ui.theme.appColors
import kotlin.math.roundToInt

/**
 * Hosts the full-screen player as a swipe-down-to-dismiss "sheet".
 *
 * Position and animation live in [PlayerSheetState]. Drags on non-scrolling areas are caught by
 * the vertical `draggable`; drags over the Queue's `LazyColumn` flow through
 * [playerSheetNestedScroll], so the sheet only starts to dismiss once the list is at the top
 * (Spotify-style).
 */
@Composable
internal fun DismissiblePlayerOverlay(
    visible: Boolean,
    viewModel: MusicPlayerViewModel,
) {
    // Window height drives the sheet's travel. Read directly (not via a full-size
    // BoxWithConstraints) so that when closed this composable emits nothing and the
    // MusicPlayerBar below stays tappable.
    val fullHeightPx = LocalWindowInfo.current.containerSize.height.toFloat()
    if (fullHeightPx <= 0f) return

    val scope = rememberCoroutineScope()
    val sheet = remember(fullHeightPx) { PlayerSheetState(fullHeightPx) }

    // `visible` (the VM flag) is the single source of truth for open/closed; the sheet
    // animates to match. Every dismiss path just flips the flag — never animates first —
    // so the sheet can't end up parked off-screen while the VM still thinks it's open.
    LaunchedEffect(visible) {
        if (visible) sheet.expand() else sheet.hide()
    }
    // Keep a closed sheet pinned off-screen if the window resizes (e.g. rotation).
    LaunchedEffect(fullHeightPx) {
        if (!visible) sheet.snapHidden()
    }

    // On release: dismiss past the threshold (flip the flag, let the effect above animate the
    // close), otherwise settle back open.
    fun onReleased(velocity: Float) {
        if (sheet.shouldDismiss(velocity)) viewModel.setFullScreen(false)
        else scope.launch { sheet.settleBack() }
    }

    // Nothing to draw — and nothing over the bar to swallow taps — once fully hidden.
    if (!visible && sheet.isHidden) return

    val nestedScroll = remember(sheet) {
        playerSheetNestedScroll(sheet, onReleased = { onReleased(it) })
    }

    Surface(
        color = appColors.backgroundSecondary,
        modifier = Modifier
            .fillMaxSize()
            .offset { IntOffset(0, sheet.offsetPx.roundToInt()) }
            .nestedScroll(nestedScroll)
            .draggable(
                orientation = Orientation.Vertical,
                state = rememberDraggableState { sheet.applyDrag(it) },
                onDragStopped = { velocity -> onReleased(velocity) }
            ),
    ) {
        MusicPlayerScreen(
            viewModel = viewModel,
            onDismissRequest = { viewModel.setFullScreen(false) }
        )
    }
}

/**
 * Vertical position + animation state for the dismissible player sheet.
 *
 * [offsetPx] runs from 0 (fully expanded) to [fullHeightPx] (off the bottom of the screen).
 * Drags mutate it synchronously via [applyDrag]; [expand]/[hide]/[settleBack] animate it.
 */
@Stable
private class PlayerSheetState(private val fullHeightPx: Float) {
    var offsetPx by mutableFloatStateOf(fullHeightPx)
        private set

    private val anim = Animatable(fullHeightPx)
    private val dismissThresholdPx = fullHeightPx * 0.4f

    /** True once parked completely off the bottom (i.e. effectively closed). */
    val isHidden: Boolean get() = offsetPx >= fullHeightPx

    /** Apply a raw drag delta, clamped to the sheet's range; returns the amount actually used. */
    fun applyDrag(delta: Float): Float {
        val target = (offsetPx + delta).coerceIn(0f, fullHeightPx)
        val used = target - offsetPx
        offsetPx = target
        return used
    }

    /** Whether a release at this offset/velocity should dismiss rather than settle back open. */
    fun shouldDismiss(velocity: Float): Boolean =
        offsetPx > dismissThresholdPx || velocity > FLING_VELOCITY_PX

    suspend fun expand() = animateOffsetTo(0f)
    suspend fun hide() = animateOffsetTo(fullHeightPx)
    suspend fun settleBack() = animateOffsetTo(0f)

    suspend fun snapHidden() {
        anim.snapTo(fullHeightPx)
        offsetPx = fullHeightPx
    }

    private suspend fun animateOffsetTo(target: Float) {
        anim.snapTo(offsetPx)
        anim.animateTo(
            targetValue = target,
            animationSpec = tween(
                durationMillis = if (target == 0f) EXPAND_MS else HIDE_MS,
                easing = FastOutSlowInEasing
            )
        ) { offsetPx = value }
    }

    private companion object {
        const val EXPAND_MS = 400
        const val HIDE_MS = 300
        const val FLING_VELOCITY_PX = 2200f
    }
}

/**
 * Routes the Queue list's scroll to the sheet: a downward drag past the top of the list pulls
 * the sheet down, and dragging back up returns it. Only [NestedScrollSource.UserInput] is
 * honoured — fling momentum must not move the sheet, or a fling that reaches the list's top
 * would dump its leftover velocity into the sheet with no callback to settle it (leaving it
 * parked off-screen). [onReleased] fires when a sheet-moving drag ends.
 */
private fun playerSheetNestedScroll(
    sheet: PlayerSheetState,
    onReleased: (velocity: Float) -> Unit,
): NestedScrollConnection = object : NestedScrollConnection {
    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        if (source != NestedScrollSource.UserInput) return Offset.Zero
        // Dragging up while the sheet is pulled down: pull it back before the list scrolls.
        if (available.y < 0f && sheet.offsetPx > 0f) return Offset(0f, sheet.applyDrag(available.y))
        return Offset.Zero
    }

    override fun onPostScroll(
        consumed: Offset,
        available: Offset,
        source: NestedScrollSource
    ): Offset {
        if (source != NestedScrollSource.UserInput) return Offset.Zero
        // List can't scroll further up: the leftover downward drag moves the sheet.
        if (available.y > 0f) return Offset(0f, sheet.applyDrag(available.y))
        return Offset.Zero
    }

    override suspend fun onPreFling(available: Velocity): Velocity {
        if (sheet.offsetPx > 0f) {
            onReleased(available.y)
            return available
        }
        return Velocity.Zero
    }
}
