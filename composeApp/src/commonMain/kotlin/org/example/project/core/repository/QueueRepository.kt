package org.example.project.core.repository

import androidx.compose.runtime.MutableState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.example.project.core.manager.MusicPlayerManager
import org.example.project.core.model.Song

class QueueRepository() {
    private val _queue = MutableStateFlow<List<Song>>(listOf())
    val queue = _queue.asStateFlow()


    fun setQueue(queue: List<Song>) {
        _queue.value = queue
    }

}
