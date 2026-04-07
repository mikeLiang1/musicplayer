package org.example.project.core.manager

import kotlinx.coroutines.flow.StateFlow
import org.example.project.core.helper.toMediaItem
import org.example.project.core.model.PlayerState
import org.example.project.core.model.Song
import org.schabi.newpipe.extractor.timeago.patterns.it

interface MusicPlayerManager {

    val playerState: StateFlow<PlayerState>

    val currentPosition: StateFlow<Long>

    fun initialise()

    fun pause()

    fun play()

    /**
     * Sets the playlist for playback.
     * @param songs List of songs to play
     * @param startIndex Index in the list to start playback from
     * @param positionMs Position in milliseconds within the starting song
     */
    fun setPlaylist(songs: List<Song>, startIndex: Int, positionMs: Long, autoPlay: Boolean)

    fun replaceFullQueueKeepingCurrentSong(songs: List<Song>, newIndex: Int)


    fun stop()

    fun seekTo(positionMs: Long)

    fun seekToDefaultPosition(index: Int)

    fun release()

    fun onAppEnteredForeground()

    fun onAppEnteredBackground()

}
