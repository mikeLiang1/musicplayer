package org.example.project.core.usecase

import org.example.project.core.manager.QueueManager
import org.example.project.core.repository.YouTubeRepository


class PlaySongUseCase(
    private val repository: YouTubeRepository,
    private val queueManager: QueueManager
) {
    suspend operator fun invoke(songUrl: String) {
        val relatedSongs = repository.getPlaylistRadio(songUrl)
        queueManager.setBaseQueue(relatedSongs)
    }
}
