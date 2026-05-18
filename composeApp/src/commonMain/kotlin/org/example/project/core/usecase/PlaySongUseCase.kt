package org.example.project.core.usecase

import org.example.project.core.manager.QueueManager
import org.example.project.core.repository.YouTubeMusicRepository
import org.example.project.core.repository.YouTubeRepository


class PlaySongUseCase(
    private val youTubeRepository: YouTubeRepository,
    private val queueManager: QueueManager,
    private val youTubeMusicRepository: YouTubeMusicRepository
) {
    suspend operator fun invoke(songUrl: String) {
        val relatedSongs = youTubeRepository.getPlaylistRadio(songUrl)
        val nextSongs = youTubeRepository.getNextRadioSongs(songUrl)
        val songs = youTubeMusicRepository.getNextRecommendations(songUrl)
        queueManager.setBaseQueue(songs)
    }
}
