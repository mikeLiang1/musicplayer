package org.example.project.core.service

import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.Player
import org.example.project.core.manager.QueueManager

class QueueForwardingPlayer(
    player: Player,
    private val queueManager: QueueManager
) : ForwardingPlayer(player) {
    override fun seekToNextMediaItem() { queueManager.playNext() }
    override fun seekToPreviousMediaItem() { queueManager.playPrevious() }
    override fun hasNextMediaItem() = queueManager.hasNext()
    override fun hasPreviousMediaItem() = queueManager.hasPrevious()
}