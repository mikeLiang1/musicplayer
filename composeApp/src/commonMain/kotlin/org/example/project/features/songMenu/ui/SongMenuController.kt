package org.example.project.features.songMenu.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import org.example.project.core.model.Song


data class SelectedMenuTarget(
    val song: Song,
    val playlistSongId: String? = null
)

class SongMenuController internal constructor(
    private val onShow: (Song, String?) -> Unit
) {
    fun show(song: Song, playlistSongId: String? = null) {
        onShow(song, playlistSongId)
    }
}


@Composable
fun rememberSongMenuController(
    options: List<SongMenuAction>
): SongMenuController {
    var target by remember { mutableStateOf<SelectedMenuTarget?>(null) }

    val controller = remember {
        SongMenuController { song, playlistSongId ->
            target = SelectedMenuTarget(song, playlistSongId)
        }
    }

    SongMenuProvider(
        selectedMenuTarget = target,
        onTargetConsumed = { target = null },
        songMenuOptions = options
    )

    return controller

}
