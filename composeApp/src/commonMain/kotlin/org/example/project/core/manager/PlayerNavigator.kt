package org.example.project.core.manager

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

/**
 * Bridges platform entry points (e.g. tapping the media notification) to the
 * Compose nav tree, which owns the full-screen player visibility state.
 */
class PlayerNavigator {
    private val openPlayerChannel = Channel<Unit>(Channel.CONFLATED)
    val openPlayerEvents: Flow<Unit> = openPlayerChannel.receiveAsFlow()

    fun requestOpenPlayer() {
        openPlayerChannel.trySend(Unit)
    }
}
