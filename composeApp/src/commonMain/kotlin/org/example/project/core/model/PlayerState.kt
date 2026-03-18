package org.example.project.core.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LastPage
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.vector.ImageVector

@Stable
data class PlayerState(
    val currentSong: Song? = null,
    val currentIndex: Int = 0,
    val isPlaying: Boolean = false,
    val queue: List<Song> = listOf(),
    val originalQueue: List<Song> = listOf(),
    val isShuffled: Boolean = false,
    val manualItemCount: Int = 0,
    val firstManualIndex: Int? = null,
    val flowMode: FlowMode = FlowMode.STOP_AT_END,
    val durationMs: Long = 0L
)

enum class FlowMode(val label: String, val icon: ImageVector) {
    STOP_AT_END(
        label = "Stop at end",
        icon = Icons.AutoMirrored.Filled.LastPage,
    ),
    REPEAT_ALL(
        label = "Repeating queue",
        icon = Icons.Rounded.Repeat,
    ),
    INFINITE(
        label = "Autoplay",
        icon = Icons.Default.AllInclusive,
    )
}
