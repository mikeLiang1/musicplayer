package org.example.project.core.service

import android.util.Log
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
    override fun seekToNext() {
        Log.d("QueueForwardingPlayer", "seekToNext() called from notification/Bluetooth")
        queueManager.playNext()
    }

    override fun seekToPrevious() {
        Log.d("QueueForwardingPlayer", "seekToPrevious() called from notification/Bluetooth")
        queueManager.playPrevious()
    }

    override fun seekToNextMediaItem() {
        Log.d("QueueForwardingPlayer", "seekToNextMediaItem() called from UI/other")
        queueManager.playNext()
    }

    override fun seekToPreviousMediaItem() {
        Log.d("QueueForwardingPlayer", "seekToPreviousMediaItem() called from UI/other")
        queueManager.playPrevious()
    }


    override fun hasNextMediaItem(): Boolean {
        Log.d("QueueForwardingPlayer", "hasNextMediaItem called")
      return queueManager.hasNext()
    }
    override fun hasPreviousMediaItem(): Boolean {
        Log.d("QueueForwardingPlayer", "hasPrevious called")
        return queueManager.hasPrevious()
    }
}
