package org.example.project.core.manager

sealed class QueueIntent {
    data class ReplaceQueue(val newIndex: Int) : QueueIntent()
    data class SeekToItem(val newIndex: Int) : QueueIntent()
    data class SeekToPreviousManual(val newIndex: Int, val offset: Int = 0) : QueueIntent()
    object NewQueue : QueueIntent()
}
