package org.example.project.core.usecase

import org.example.project.core.manager.QueueManager
import org.example.project.core.repository.InnerTubeRepository


class PlaySongUseCase(
    private val innerTubeRepository: InnerTubeRepository,
    private val queueManager: QueueManager,
) {
    suspend operator fun invoke(songUrl: String): Result<Unit> = runCatching {
        val songs = innerTubeRepository.getRecommendations(songUrl)
        queueManager.setBaseQueue(songs.songs)
    }
}
