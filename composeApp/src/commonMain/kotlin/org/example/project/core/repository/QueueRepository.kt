package org.example.project.core.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.example.project.core.model.Song


data class QueueUpdateResult(
    val queue: List<Song>,
    val currentIndex: Int
)

class QueueRepository {
    private val _queue = MutableStateFlow<List<Song>>(listOf())
    val queue = _queue.asStateFlow()
    private val _isShuffled = MutableStateFlow(false)
    val isShuffle = _isShuffled.asStateFlow()

    private val _manualQueue = MutableStateFlow<List<Song>>(listOf())
    val manual = _manualQueue.asStateFlow()

    private val manualQueueIds = setOf<String>()

    private var originalQueue = listOf<Song>()


    fun setQueue(queue: List<Song>) {
        _isShuffled.value = false
        _queue.value = queue
    }

    fun addToQueue() {

    }

    fun shuffleClicked(currentIndex: Int): QueueUpdateResult {
        _isShuffled.value = !_isShuffled.value
        val queue = _queue.value

        if (_isShuffled.value) {
            originalQueue = queue  // snapshot before shuffling
            val played = queue.subList(0, currentIndex + 1)
            val upcoming = queue.subList(currentIndex + 1, queue.size)
                .filter { it.url !in manualQueueIds }
            _queue.value = played + _manualQueue.value + upcoming.shuffled()
            return  QueueUpdateResult(_queue.value, currentIndex)
        } else {
            val currentSong = queue[currentIndex]
            val currentOriginalIndex = originalQueue.indexOfFirst { it.url == currentSong.url }

            val played = originalQueue.subList(0, currentOriginalIndex + 1)
            val upcoming = originalQueue.subList(currentOriginalIndex + 1, originalQueue.size)
                .filter { it.url !in manualQueueIds }

            _queue.value = played + _manualQueue.value + upcoming

            return QueueUpdateResult(_queue.value, currentOriginalIndex)
        }
    }

    private fun shuffleQueue() {

    }

    private fun unShuffleQueue() {

    }

}
