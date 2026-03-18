package org.example.project.features.musicPlayer.model

import org.example.project.core.model.PlayerState
import org.example.project.core.model.Song

data class PlayerQueue(
    val history: List<Song> = listOf(),
    val current: Song? = null,
    val manual: List<Song> = listOf(),
    val upcoming: List<Song> = listOf()
) {
    val allSongs get() = history + listOfNotNull(current) + manual + upcoming

    fun absoluteIndexOf(song: Song) = allSongs.indexOfFirst { it.uniqueId == song.uniqueId }

    companion object {

        fun from(state: PlayerState): PlayerQueue {
            val queue = state.queue
            val ci = state.currentIndex.coerceIn(0, queue.lastIndex)
            return PlayerQueue(
                history  = queue.take(ci),
                current  = queue.getOrNull(ci),
                manual   = queue.drop(ci + 1).take(state.manualItemCount),
                upcoming = queue.drop(ci + 1 + state.manualItemCount)
            )
        }

    }
}
