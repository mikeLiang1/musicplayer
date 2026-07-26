package org.example.project.features.musicPlayer.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.example.project.ui.theme.AppPreview
import org.example.project.ui.theme.DevicePreviews
import org.example.project.ui.theme.Dimens
import org.example.project.ui.theme.appColors
import kotlin.time.Clock

private val sleepTimerDurationsMinutes = listOf(15, 30, 45, 60)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SleepTimerBottomSheet(
    isVisible: Boolean,
    sleepTimerEndAtMs: Long?,
    sleepTimerEndOfTrack: Boolean,
    onDismissRequest: () -> Unit,
    onDurationSelected: (Int) -> Unit,
    onEndOfTrackSelected: () -> Unit,
    onCancelTimer: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    LaunchedEffect(isVisible) {
        if (isVisible) sheetState.show() else sheetState.hide()
    }

    fun closeAfter(action: () -> Unit) {
        scope.launch { sheetState.hide() }.invokeOnCompletion {
            if (!sheetState.isVisible) onDismissRequest()
        }
        action()
    }

    if (isVisible) {
        ModalBottomSheet(
            onDismissRequest = onDismissRequest,
            sheetState = sheetState,
            containerColor = appColors.backgroundElevated,
            dragHandle = {
                Surface(
                    modifier = Modifier.padding(vertical = Dimens.spaceM),
                    color = appColors.divider,
                    shape = MaterialTheme.shapes.extraLarge,
                ) {
                    Box(Modifier.size(width = Dimens.Size.dragHandleWidth, height = Dimens.spaceXs))
                }
            }
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(bottom = Dimens.spaceL)) {
                Text(
                    text = "Sleep timer",
                    style = MaterialTheme.typography.titleMedium,
                    color = appColors.textPrimary,
                    modifier = Modifier.padding(horizontal = Dimens.spaceL, vertical = Dimens.spaceM)
                )

                sleepTimerStatusLabel(sleepTimerEndAtMs, sleepTimerEndOfTrack)?.let { label ->
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodySmall,
                        color = appColors.accentPrimary,
                        modifier = Modifier.padding(horizontal = Dimens.spaceL, vertical = Dimens.spaceXs)
                    )
                }

                if (sleepTimerEndAtMs != null || sleepTimerEndOfTrack) {
                    SleepTimerOptionRow(text = "Turn off timer") {
                        closeAfter(onCancelTimer)
                    }
                }

                SleepTimerOptionRow(text = "End of current song") {
                    closeAfter(onEndOfTrackSelected)
                }

                sleepTimerDurationsMinutes.forEach { minutes ->
                    SleepTimerOptionRow(text = "$minutes minutes") {
                        closeAfter { onDurationSelected(minutes) }
                    }
                }
            }
        }
    }
}

@Composable
private fun sleepTimerStatusLabel(endAtMs: Long?, endOfTrack: Boolean): String? {
    if (endOfTrack) return "Playback will pause at the end of this song"
    if (endAtMs == null) return null

    var remainingMinutes by remember(endAtMs) {
        mutableStateOf(minutesUntil(endAtMs))
    }
    LaunchedEffect(endAtMs) {
        while (true) {
            remainingMinutes = minutesUntil(endAtMs)
            delay(30_000L)
        }
    }
    return "Playback will pause in $remainingMinutes min"
}

private fun minutesUntil(endAtMs: Long): Int =
    ((endAtMs - Clock.System.now().toEpochMilliseconds()) / 60_000L).toInt().coerceAtLeast(0)

@Composable
private fun SleepTimerOptionRow(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = Dimens.spaceL, vertical = Dimens.spaceM)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = appColors.textPrimary
        )
    }
}

@DevicePreviews
@Composable
private fun SleepTimerBottomSheetPreview() {
    AppPreview {
        SleepTimerBottomSheet(
            isVisible = true,
            sleepTimerEndAtMs = null,
            sleepTimerEndOfTrack = false,
            onDismissRequest = {},
            onDurationSelected = {},
            onEndOfTrackSelected = {},
            onCancelTimer = {}
        )
    }
}
