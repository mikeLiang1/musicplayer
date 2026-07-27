package org.example.project.core.usecase

import org.example.project.core.manager.QueueManager
import org.example.project.core.model.QueueContext
import org.example.project.core.model.QueueContextType
import org.example.project.core.repository.InnerTubeRepository


class PlaySongUseCase(
    private val innerTubeRepository: InnerTubeRepository,
    private val queueManager: QueueManager,
) {
    suspend operator fun invoke(songUrl: String): Result<Unit> = runCatching {
        val songs = innerTubeRepository.getRecommendations(songUrl)
        // InnerTube returns the radio with its seed song first, so that title names the radio.
        val seed = songs.songs.firstOrNull()
        queueManager.setBaseQueue(
            songs = songs.songs,
            context = seed?.let {
                QueueContext(id = songUrl, type = QueueContextType.RADIO, title = it.title)
            }
        )
    }
}
