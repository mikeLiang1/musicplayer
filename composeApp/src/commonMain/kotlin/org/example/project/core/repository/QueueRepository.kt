package org.example.project.core.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.example.project.core.model.Song


data class QueueUpdateResult(
    val queue: List<Song>,
    val currentIndex: Int
)

class QueueRepository(
    val savedDataRepository: SavedDataRepository,
    ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val scope = CoroutineScope(ioDispatcher + SupervisorJob())
    private val _queue = MutableStateFlow<List<Song>>(listOf())
    val queue = _queue.asStateFlow()
    private val _isShuffled = MutableStateFlow(false)
    val isShuffle = _isShuffled.asStateFlow()

    private val _manualQueue = MutableStateFlow<List<Song>>(listOf())
    val manual = _manualQueue.asStateFlow()

    private val manualQueueIds = setOf<String>()

    private var originalQueue = listOf<Song>()


    fun setQueue(queue: List<Song>, isShuffled: Boolean, savedOriginalQueue: List<Song>? = null) {
        _isShuffled.value = isShuffled
        _queue.value = queue
        savedOriginalQueue?.let {
            originalQueue = it
        }
    }

    fun addToQueue() {

    }

    // Returns the new index after shuffling
    fun shuffleClicked(currentIndex: Int): QueueUpdateResult {
        val queue = _queue.value

        // Currently not shuffled, shuffle everything after curentindex
        val queueUpdateResult = if (!_isShuffled.value) {
            originalQueue = queue  // snapshot before shuffling
            val played = queue.subList(0, currentIndex + 1)
            val upcoming = queue.subList(currentIndex + 1, queue.size)
                .filter { it.url !in manualQueueIds }
            _queue.value = played + _manualQueue.value + upcoming.shuffled()
            QueueUpdateResult(_queue.value, currentIndex)
        }
        // Currently shuffled, to unshuffle, find the index where current song is playing, and restore
        // before and after
        else {
            val currentSong = queue[currentIndex]
            val currentOriginalIndex = originalQueue.indexOfFirst { it.url == currentSong.url }

            val played = originalQueue.subList(0, currentOriginalIndex + 1)
            val upcoming = originalQueue.subList(currentOriginalIndex + 1, originalQueue.size)
                .filter { it.url !in manualQueueIds }

            _queue.value = played + _manualQueue.value + upcoming

            QueueUpdateResult(_queue.value, currentOriginalIndex)
        }
        // Only when we successfully update, change the value
        _isShuffled.value = !_isShuffled.value
        scope.launch {
            savedDataRepository.saveOriginalQueue(originalQueue)
            savedDataRepository.saveIsShuffled(_isShuffled.value)
            savedDataRepository.saveIndex(queueUpdateResult.currentIndex)
        }
        return queueUpdateResult
    }

}
