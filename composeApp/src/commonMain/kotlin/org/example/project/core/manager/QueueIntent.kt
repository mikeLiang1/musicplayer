package org.example.project.core.manager

sealed class QueueIntent {
    data class ReplaceQueue(val newIndex: Int) : QueueIntent()
    data class SeekToItem(val newIndex: Int) : QueueIntent()
    data class SeekAndRebuild(val mediaIndex: Int, val queueIndex: Int) : QueueIntent()
    data class NewQueue(val positionMs: Long = 0) : QueueIntent()
}
