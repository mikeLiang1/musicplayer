package org.example.project.core.service

import androidx.annotation.OptIn
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import org.example.project.core.manager.QueueManager

@UnstableApi
class QueueForwardingPlayer @OptIn(UnstableApi::class) constructor
    (
    player: Player,
    private val queueManager: QueueManager
) : ForwardingPlayer(player) {
    override fun seekToNextMediaItem() {
        queueManager.playNext()
    }

    override fun seekToPreviousMediaItem() {
        queueManager.playPrevious()
    }

    override fun hasNextMediaItem() = queueManager.hasNext()
    override fun hasPreviousMediaItem() = queueManager.hasPrevious()
}
