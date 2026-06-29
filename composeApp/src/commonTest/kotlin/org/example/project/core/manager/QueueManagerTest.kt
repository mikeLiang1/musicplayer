package org.example.project.core.manager

import kotlinx.coroutines.runBlocking
import org.example.project.core.model.Song
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals

class QueueManagerTest {

    private lateinit var manager: QueueManager
    private val collectedIntents = mutableListOf<QueueIntent>()

    // ── Test Fixtures ──────────────────────────────────────────────────────────
    private val song1 = Song(
        uniqueId = "song1",
        url = "url1",
        title = "Song One",
        artist = "Artist A",
        thumbnailUrl = "thumb1",
        duration = 200000L
    )
    private val song2 = Song(
        uniqueId = "song2",
        url = "url2",
        title = "Song Two",
        artist = "Artist A",
        thumbnailUrl = "thumb2",
        duration = 180000L
    )
    private val song3 = Song(
        uniqueId = "song3",
        url = "url3",
        title = "Song Three",
        artist = "Artist B",
        thumbnailUrl = "thumb3",
        duration = 240000L
    )
    private val song4 = Song(
        uniqueId = "song4",
        url = "url4",
        title = "Song Four",
        artist = "Artist C",
        thumbnailUrl = "thumb4",
        duration = 300000L
    )
    private val song5 = Song(
        uniqueId = "song5",
        url = "url5",
        title = "Song Five",
        artist = "Artist A",
        thumbnailUrl = "thumb5",
        duration = 150000L
    )
    private val allSongs = listOf(song1, song2, song3, song4, song5)

    /**
     * Drains all pending intents from the channel into [collectedIntents].
     * Must be called after each operation that emits intents, before assertions.
     */
    private fun drain() {
        while (true) {
            manager._intent.tryReceive().getOrNull()?.let { collectedIntents.add(it) } ?: break
        }
    }

    @BeforeTest
    fun setup() {
        manager = QueueManager()
        collectedIntents.clear()
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  QueueState — Computed Property Tests
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    fun `queueState initial default values`() {
        val state = manager.queueState.value
        assertTrue(state.baseQueue.isEmpty())
        assertTrue(state.manualQueue.isEmpty())
        assertEquals(0, state.currentBaseIndex)
        assertNull(state.currentManualSong)
        assertFalse(state.isShuffled)
        assertEquals(PlaybackMode.OFF, state.playbackMode)
        assertNull(state.contextId)
    }

    @Test
    fun `current returns base song when no manual song`() {
        val state = QueueState(baseQueue = allSongs, currentBaseIndex = 2)
        assertEquals(song3, state.current)
    }

    @Test
    fun `current returns manual song when present`() {
        val state = QueueState(
            baseQueue = allSongs,
            currentBaseIndex = 1,
            currentManualSong = song5
        )
        assertEquals(song5, state.current)
    }

    @Test
    fun `current returns null for empty base and no manual`() {
        val state = QueueState()
        assertNull(state.current)
    }

    @Test
    fun `history excludes current base song when not playing manual`() {
        val state = QueueState(baseQueue = allSongs, currentBaseIndex = 2)
        assertEquals(listOf(song1, song2), state.history)
    }

    @Test
    fun `history includes current base song when playing manual`() {
        val state = QueueState(
            baseQueue = allSongs,
            currentBaseIndex = 2,
            currentManualSong = song5
        )
        assertEquals(listOf(song1, song2, song3), state.history)
    }

    @Test
    fun `manualUpNext returns manualQueue`() {
        val state = QueueState(manualQueue = listOf(song4, song5))
        assertEquals(listOf(song4, song5), state.manualUpNext)
    }

    @Test
    fun `normalUpNext returns songs after currentBaseIndex`() {
        val state = QueueState(baseQueue = allSongs, currentBaseIndex = 2)
        assertEquals(listOf(song4, song5), state.normalUpNext)
    }

    @Test
    fun `normalUpNext empty at end of queue`() {
        val state = QueueState(baseQueue = allSongs, currentBaseIndex = 4)
        assertTrue(state.normalUpNext.isEmpty())
    }

    @Test
    fun `playbackQueue assembles flat list correctly with manual song`() {
        val state = QueueState(
            baseQueue = allSongs,
            currentBaseIndex = 1,
            currentManualSong = song5,
            manualQueue = listOf(song3)
        )
        val pq = state.playbackQueue
        assertEquals(7, pq.size)
        assertEquals(song1, pq[0])
        assertEquals(song2, pq[1])
        assertEquals(song5, pq[2])
        assertEquals(song3, pq[3])
        assertEquals(song3, pq[4])
        assertEquals(song4, pq[5])
        assertEquals(song5, pq[6])
    }

    @Test
    fun `playbackQueue assembles flat list correctly without manual`() {
        val state = QueueState(baseQueue = allSongs, currentBaseIndex = 2)
        assertEquals(allSongs, state.playbackQueue)
    }

    @Test
    fun `playbackCurrentIndex equals history size`() {
        val state = QueueState(baseQueue = allSongs, currentBaseIndex = 2)
        assertEquals(2, state.playbackCurrentIndex)
    }

    @Test
    fun `playbackCurrentIndex includes base index when manual`() {
        val state = QueueState(
            baseQueue = allSongs,
            currentBaseIndex = 2,
            currentManualSong = song5
        )
        assertEquals(3, state.playbackCurrentIndex)
    }

    @Test
    fun `seenIds set correctly on setBaseQueue`() {
        manager.setBaseQueue(allSongs)
        val state = manager.queueState.value
        assertEquals(allSongs.map { it.uniqueId }.toSet(), state.seenIds)
    }

    @Test
    fun `appendRadioSongs filters seen ids`() {
        manager.setBaseQueue(listOf(song1, song2))
        manager.appendRadioSongs(listOf(song2, song3, song4))
        val state = manager.queueState.value
        assertEquals(listOf(song1, song2, song3, song4), state.baseQueue)
        assertEquals(setOf("song1", "song2", "song3", "song4"), state.seenIds)
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  setBaseQueue
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    fun `setBaseQueue sets queue and resets manual state`() {
        manager.addToManualQueue(song5)
        assertEquals(1, manager.queueState.value.manualQueue.size)

        manager.setBaseQueue(allSongs, contextId = "test-context", currentBaseIndex = 2)
        drain()

        val state = manager.queueState.value
        assertEquals(allSongs, state.baseQueue)
        assertEquals(2, state.currentBaseIndex)
        assertTrue(state.manualQueue.isEmpty())
        assertNull(state.currentManualSong)
        assertEquals("test-context", state.contextId)
        assertTrue(state.autoPlay)
        assertFalse(state.isShuffled)

        assertTrue(collectedIntents.isNotEmpty())
        assertEquals(QueueIntent.NewQueue::class, collectedIntents.last()::class)
    }

    @Test
    fun `setBaseQueue with default index starts at 0`() {
        manager.setBaseQueue(allSongs)
        assertEquals(0, manager.queueState.value.currentBaseIndex)
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  playNext — Base Queue (no manual)
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    fun `playNext advances base index when no manual songs`() {
        manager.setBaseQueue(allSongs)
        drain()
        collectedIntents.clear()

        manager.playNext()
        drain()

        val state = manager.queueState.value
        assertEquals(1, state.currentBaseIndex)
        assertEquals(song2, state.current)
        assertNull(state.currentManualSong)

        assertTrue(collectedIntents.isNotEmpty())
        val lastIntent = collectedIntents.last()
        assertTrue(lastIntent is QueueIntent.SeekToItem)
        assertEquals(1, (lastIntent as QueueIntent.SeekToItem).newIndex)
    }

    @Test
    fun `playNext multiple times advances through base queue`() {
        manager.setBaseQueue(allSongs)
        manager.playNext()
        manager.playNext()
        manager.playNext()
        assertEquals(3, manager.queueState.value.currentBaseIndex)
        assertEquals(song4, manager.queueState.value.current)
    }

    @Test
    fun `playNext at end of base queue stays at last index`() {
        manager.setBaseQueue(allSongs, currentBaseIndex = 4)
        manager.playNext()
        val state = manager.queueState.value
        assertEquals(4, state.currentBaseIndex)
        assertEquals(song5, state.current)
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  playNext — Manual Queue Transitions
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    fun `playNext into manual song when manualQueue not empty`() {
        manager.setBaseQueue(listOf(song1, song2))
        manager.addToManualQueue(song4)
        drain()
        collectedIntents.clear()

        manager.playNext()
        drain()

        val state = manager.queueState.value
        assertNotNull(state.currentManualSong)
        assertEquals(song4.title, state.currentManualSong!!.title)
        assertTrue(state.manualQueue.isEmpty())
        assertEquals(0, state.currentBaseIndex)

        assertTrue(collectedIntents.isNotEmpty())
        val lastIntent = collectedIntents.last()
        assertTrue(lastIntent is QueueIntent.SeekToItem, "Expected SeekToItem, got ${lastIntent::class.simpleName}")
    }

    @Test
    fun `playNext from manual song to next manual song`() {
        manager.setBaseQueue(listOf(song1, song2))
        manager.addToManualQueue(song4)
        manager.addToManualQueue(song5)
        manager.playNext()

        manager.playNext()

        val state = manager.queueState.value
        assertNotNull(state.currentManualSong)
        assertEquals(song5.title, state.currentManualSong!!.title)
        assertTrue(state.manualQueue.isEmpty())
        assertEquals(0, state.currentBaseIndex)
    }

    @Test
    fun `playNext from last manual song advances base queue`() {
        manager.setBaseQueue(listOf(song1, song2, song3), currentBaseIndex = 0)
        manager.addToManualQueue(song5)
        manager.playNext()

        manager.playNext()

        val state = manager.queueState.value
        assertNull(state.currentManualSong)
        assertEquals(1, state.currentBaseIndex)
        assertEquals(song2, state.current)
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  playPrevious
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    fun `playPrevious goes back one index`() {
        manager.setBaseQueue(allSongs, currentBaseIndex = 3)
        manager.playPrevious()
        val state = manager.queueState.value
        assertEquals(2, state.currentBaseIndex)
        assertEquals(song3, state.current)
        assertNull(state.currentManualSong)
    }

    @Test
    fun `playPrevious at index 0 stays at 0`() {
        manager.setBaseQueue(allSongs, currentBaseIndex = 0)
        manager.playPrevious()
        assertEquals(0, manager.queueState.value.currentBaseIndex)
    }

    @Test
    fun `playPrevious when manual song is playing`() {
        manager.setBaseQueue(listOf(song1, song2))
        manager.addToManualQueue(song4)
        manager.playNext()

        manager.playPrevious()

        val state = manager.queueState.value
        assertNull(state.currentManualSong)
        assertEquals(0, state.currentBaseIndex)
        assertEquals(song1, state.current)
    }

    @Test
    fun `playPrevious multiple times goes back`() {
        manager.setBaseQueue(allSongs, currentBaseIndex = 4)
        manager.playPrevious()
        assertEquals(3, manager.queueState.value.currentBaseIndex)
        manager.playPrevious()
        assertEquals(2, manager.queueState.value.currentBaseIndex)
        manager.playPrevious()
        assertEquals(1, manager.queueState.value.currentBaseIndex)
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  addToManualQueue
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    fun `addToManualQueue adds song and emits ReplaceQueue`() {
        manager.setBaseQueue(allSongs)
        drain()
        collectedIntents.clear()

        manager.addToManualQueue(song4)
        drain()

        val state = manager.queueState.value
        assertEquals(1, state.manualQueue.size)
        assertEquals(song4.title, state.manualQueue[0].title)

        assertTrue(collectedIntents.isNotEmpty())
        val lastIntent = collectedIntents.last()
        assertTrue(lastIntent is QueueIntent.ReplaceQueue, "Expected ReplaceQueue, got ${lastIntent?.let { it::class.simpleName }}")
        assertEquals(state.playbackCurrentIndex, (lastIntent as QueueIntent.ReplaceQueue).newIndex)
    }

    @Test
    fun `addToManualQueue emits ReplaceQueue with correct index when manual song playing`() {
        manager.setBaseQueue(listOf(song1, song2))
        manager.addToManualQueue(song4)
        drain()
        collectedIntents.clear()

        manager.playNext()
        drain()
        collectedIntents.clear()

        manager.addToManualQueue(song5)
        drain()

        assertTrue(collectedIntents.isNotEmpty())
        val lastIntent = collectedIntents.last()
        assertTrue(lastIntent is QueueIntent.ReplaceQueue, "Expected ReplaceQueue, got ${lastIntent::class.simpleName}")
        assertEquals(1, (lastIntent as QueueIntent.ReplaceQueue).newIndex)
    }

    @Test
    fun `addToManualQueue generates new uniqueId for the added song`() {
        manager.setBaseQueue(allSongs)
        manager.addToManualQueue(song4)

        val manualSong = manager.queueState.value.manualQueue[0]
        assertNotEquals(song4.uniqueId, manualSong.uniqueId)
    }

    @Test
    fun `addToManualQueue appends multiple songs`() {
        manager.setBaseQueue(allSongs)
        manager.addToManualQueue(song4)
        manager.addToManualQueue(song5)

        assertEquals(2, manager.queueState.value.manualQueue.size)
    }

    @Test
    fun `addToManualQueue adds song when base queue is empty`() {
        manager.addToManualQueue(song1)
        assertEquals(1, manager.queueState.value.manualQueue.size)
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  playSongFromQueue
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    fun `playSongFromQueue does nothing when song already playing`() {
        manager.setBaseQueue(allSongs, currentBaseIndex = 2)
        drain()
        collectedIntents.clear()

        manager.playSongFromQueue(song3.uniqueId)
        drain()

        assertTrue(collectedIntents.isEmpty())
        assertEquals(2, manager.queueState.value.currentBaseIndex)
    }

    @Test
    fun `playSongFromQueue selects from normal up next`() {
        manager.setBaseQueue(allSongs, currentBaseIndex = 1)
        drain()
        collectedIntents.clear()

        manager.playSongFromQueue(song4.uniqueId)
        drain()

        assertEquals(3, manager.queueState.value.currentBaseIndex)
        assertEquals(song4, manager.queueState.value.current)

        assertTrue(collectedIntents.isNotEmpty())
        assertTrue(collectedIntents.last() is QueueIntent.SeekToItem)
    }

    @Test
    fun `playSongFromQueue selects from manual queue`() {
        manager.setBaseQueue(listOf(song1, song2))
        manager.addToManualQueue(song4)
        manager.addToManualQueue(song5)
        val manualSongId = manager.queueState.value.manualQueue[0].uniqueId
        drain()
        collectedIntents.clear()

        manager.playSongFromQueue(manualSongId)
        drain()

        val state = manager.queueState.value
        assertNotNull(state.currentManualSong)
        assertEquals(song4.title, state.currentManualSong?.title)
        assertEquals(1, state.manualQueue.size)
        assertTrue(collectedIntents.isNotEmpty(), "Expected intents, got $collectedIntents")
        assertTrue(collectedIntents.last() is QueueIntent.SeekAndRebuild, "Expected SeekAndRebuild, got ${collectedIntents.last()::class.simpleName}")
    }

    @Test
    fun `playSongFromQueue selects from history`() {
        manager.setBaseQueue(allSongs, currentBaseIndex = 4)
        manager.playSongFromQueue(song1.uniqueId)
        assertEquals(0, manager.queueState.value.currentBaseIndex)
        assertEquals(song1, manager.queueState.value.current)
    }

    @Test
    fun `playSongFromQueue selects from manual queue when already playing manual`() {
        manager.setBaseQueue(listOf(song1, song2))
        manager.addToManualQueue(song4)
        manager.addToManualQueue(song5)
        manager.playNext()
        val remainingId = manager.queueState.value.manualQueue[0].uniqueId

        manager.playSongFromQueue(remainingId)

        val state = manager.queueState.value
        assertNotNull(state.currentManualSong)
        assertEquals(song5.title, state.currentManualSong!!.title)
        assertTrue(state.manualQueue.isEmpty(), "Expected empty manual queue, got ${state.manualQueue.size}")
    }

    @Test
    fun `playSongFromQueue does nothing for unknown id`() {
        manager.setBaseQueue(allSongs)
        drain()
        collectedIntents.clear()

        manager.playSongFromQueue("nonexistent-id")
        drain()

        assertTrue(collectedIntents.isEmpty())
        assertEquals(0, manager.queueState.value.currentBaseIndex)
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  removeSong
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    fun `removeSong removes from manual queue and emits ReplaceQueue`() {
        manager.setBaseQueue(allSongs)
        manager.addToManualQueue(song4)
        manager.addToManualQueue(song5)
        val manualId = manager.queueState.value.manualQueue[0].uniqueId
        drain()
        collectedIntents.clear()

        manager.removeSong(manualId)
        drain()

        assertEquals(1, manager.queueState.value.manualQueue.size)
        assertEquals(song5.title, manager.queueState.value.manualQueue[0].title)

        assertTrue(collectedIntents.isNotEmpty())
        val lastIntent = collectedIntents.last()
        assertTrue(lastIntent is QueueIntent.ReplaceQueue, "Expected ReplaceQueue, got ${lastIntent::class.simpleName}")
        assertEquals(0, (lastIntent as QueueIntent.ReplaceQueue).newIndex)
    }

    @Test
    fun `removeSong removes from upcoming base queue and emits ReplaceQueue`() {
        manager.setBaseQueue(allSongs, currentBaseIndex = 2)
        drain()
        collectedIntents.clear()

        manager.removeSong(song4.uniqueId)
        drain()

        val state = manager.queueState.value
        assertEquals(listOf(song1, song2, song3, song5), state.baseQueue)
        assertEquals(2, state.currentBaseIndex)

        assertTrue(collectedIntents.isNotEmpty())
        val lastIntent = collectedIntents.last()
        assertTrue(lastIntent is QueueIntent.ReplaceQueue, "Expected ReplaceQueue, got ${lastIntent::class.simpleName}")
        assertEquals(2, (lastIntent as QueueIntent.ReplaceQueue).newIndex)
    }

    @Test
    fun `removeSong does not remove current or history songs`() {
        manager.setBaseQueue(allSongs, currentBaseIndex = 2)
        manager.removeSong(song3.uniqueId)
        manager.removeSong(song2.uniqueId)

        assertEquals(allSongs, manager.queueState.value.baseQueue)
        assertEquals(2, manager.queueState.value.currentBaseIndex)
    }

    @Test
    fun `removeSong does nothing for unknown id`() {
        manager.setBaseQueue(allSongs)
        manager.removeSong("nonexistent-id")
        assertEquals(allSongs, manager.queueState.value.baseQueue)
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  Shuffle / Unshuffle
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    fun `shuffle randomizes upcoming songs`() {
        manager.setBaseQueue(allSongs, currentBaseIndex = 1)

        manager.shuffle()

        val state = manager.queueState.value
        assertTrue(state.isShuffled)
        assertEquals(song2, state.current)
        assertEquals(listOf(song1), state.baseQueue.take(1))
        assertNotNull(state.preShuffleBaseQueue)
        assertEquals(1, state.preShuffleBaseIndex)
        val afterCurrent = state.baseQueue.drop(2)
        assertEquals(3, afterCurrent.size)
        assertEquals(setOf("song3", "song4", "song5"), afterCurrent.map { it.uniqueId }.toSet())
    }

    @Test
    fun `shuffle does nothing for empty queue`() {
        manager.shuffle()
        assertFalse(manager.queueState.value.isShuffled)
    }

    @Test
    fun `shuffle at last index marks shuffled with full snapshot and no reorder`() {
        // There is nothing after the current song to shuffle, but the toggle should still
        // engage (previously this was a dead button) and snapshot the original order.
        manager.setBaseQueue(allSongs, currentBaseIndex = 4)
        manager.shuffle()

        val state = manager.queueState.value
        assertTrue(state.isShuffled)
        assertEquals(allSongs, state.baseQueue)
        assertEquals(allSongs, state.preShuffleBaseQueue)
    }

    @Test
    fun `shuffle is a no-op when already shuffled`() {
        manager.setBaseQueue(allSongs, currentBaseIndex = 1)
        manager.shuffle()
        val afterFirst = manager.queueState.value

        manager.shuffle()
        val afterSecond = manager.queueState.value

        // The second shuffle must not re-randomize or clobber the original snapshot.
        assertEquals(afterFirst.baseQueue, afterSecond.baseQueue)
        assertEquals(allSongs, afterSecond.preShuffleBaseQueue)
    }

    @Test
    fun `appendRadioSongs keeps preShuffleBaseQueue in sync when shuffled`() {
        manager.setBaseQueue(listOf(song1, song2, song3), currentBaseIndex = 0)
        manager.shuffle()

        val song6 = song1.copy(uniqueId = "song6", title = "Song Six")
        manager.appendRadioSongs(listOf(song6))

        val state = manager.queueState.value
        // New radio song lands in both the live queue and the snapshot, so it survives unshuffle.
        assertTrue(state.baseQueue.any { it.uniqueId == "song6" })
        assertEquals(listOf("song1", "song2", "song3", "song6"), state.preShuffleBaseQueue?.map { it.uniqueId })
    }

    @Test
    fun `unshuffle after appendRadioSongs restores original order including radio songs`() {
        manager.setBaseQueue(listOf(song1, song2, song3), currentBaseIndex = 0)
        manager.shuffle()
        val song6 = song1.copy(uniqueId = "song6", title = "Song Six")
        manager.appendRadioSongs(listOf(song6))

        manager.unshuffle()

        val state = manager.queueState.value
        assertFalse(state.isShuffled)
        assertEquals(listOf("song1", "song2", "song3", "song6"), state.baseQueue.map { it.uniqueId })
        assertNull(state.preShuffleBaseQueue)
    }

    @Test
    fun `removeSong removes from snapshot so it does not reappear on unshuffle`() {
        manager.setBaseQueue(allSongs, currentBaseIndex = 0)
        manager.shuffle()

        manager.removeSong(song4.uniqueId)
        manager.unshuffle()

        val state = manager.queueState.value
        assertFalse(state.baseQueue.any { it.uniqueId == song4.uniqueId })
        assertNull(state.preShuffleBaseQueue)
    }

    @Test
    fun `replaceQueuesPreservingState reconciles snapshot to new base set when shuffled`() {
        manager.setBaseQueue(allSongs, currentBaseIndex = 0)
        manager.shuffle()

        // Drag-reorder result: a permutation of the same songs minus none.
        val reordered = listOf(song1, song5, song4, song3, song2)
        manager.replaceQueuesPreservingState(reordered, emptyList(), currentBaseIndex = 0)

        val snapshotIds = manager.queueState.value.preShuffleBaseQueue?.map { it.uniqueId }?.toSet()
        assertEquals(allSongs.map { it.uniqueId }.toSet(), snapshotIds)
    }

    @Test
    fun `unshuffle clears shuffle when current song is not in snapshot`() {
        // Defensive: an inconsistent restored state where current isn't in the snapshot
        // should clear shuffle on the live queue rather than wedging the toggle.
        manager.restoreState(
            QueueState(
                baseQueue = listOf(song4, song5),
                currentBaseIndex = 0,
                isShuffled = true,
                preShuffleBaseQueue = listOf(song1, song2, song3)
            ),
            positionMs = 0L
        )

        manager.unshuffle()

        val state = manager.queueState.value
        assertFalse(state.isShuffled)
        assertNull(state.preShuffleBaseQueue)
        assertEquals(listOf(song4, song5), state.baseQueue)
    }

    @Test
    fun `unshuffle restores original order`() {
        manager.setBaseQueue(allSongs, currentBaseIndex = 2)
        manager.shuffle()

        manager.unshuffle()

        val state = manager.queueState.value
        assertFalse(state.isShuffled)
        assertEquals(allSongs, state.baseQueue)
        assertNull(state.preShuffleBaseQueue)
        assertNull(state.preShuffleBaseIndex)
        assertEquals(2, state.currentBaseIndex)
        assertEquals(song3, state.current)
    }

    @Test
    fun `unshuffle does nothing when not shuffled`() {
        manager.setBaseQueue(allSongs)
        val stateBefore = manager.queueState.value

        manager.unshuffle()

        assertEquals(stateBefore, manager.queueState.value)
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  togglePlaybackMode
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    fun `togglePlaybackMode cycles OFF to REPEAT`() {
        manager.togglePlaybackMode()
        assertEquals(PlaybackMode.REPEAT, manager.queueState.value.playbackMode)
    }

    @Test
    fun `togglePlaybackMode cycles REPEAT to Infinite`() {
        manager.togglePlaybackMode()
        manager.togglePlaybackMode()
        assertEquals(PlaybackMode.Infinite, manager.queueState.value.playbackMode)
    }

    @Test
    fun `togglePlaybackMode cycles Infinite to OFF`() {
        manager.togglePlaybackMode()
        manager.togglePlaybackMode()
        manager.togglePlaybackMode()
        assertEquals(PlaybackMode.OFF, manager.queueState.value.playbackMode)
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  hasNext / hasPrevious
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    fun `hasNext true when more base songs remain`() {
        manager.setBaseQueue(allSongs, currentBaseIndex = 2)
        assertTrue(manager.hasNext())
    }

    @Test
    fun `hasNext false at end of queue with no repeat`() {
        manager.setBaseQueue(allSongs, currentBaseIndex = 4)
        assertFalse(manager.hasNext())
    }

    @Test
    fun `hasNext true when manual queue is not empty`() {
        manager.setBaseQueue(allSongs, currentBaseIndex = 4)
        manager.addToManualQueue(song4)
        assertTrue(manager.hasNext())
    }

    @Test
    fun `hasNext true when currently playing manual song`() {
        manager.setBaseQueue(listOf(song1, song2))
        manager.addToManualQueue(song4)
        manager.playNext()
        assertTrue(manager.hasNext())
    }

    @Test
    fun `hasNext true with Infinite repeat`() {
        manager.setBaseQueue(allSongs, currentBaseIndex = 4)
        manager.togglePlaybackMode()
        manager.togglePlaybackMode()
        assertTrue(manager.hasNext())
    }

    @Test
    fun `hasNext true with REPEAT mode and non-empty queue`() {
        manager.setBaseQueue(allSongs, currentBaseIndex = 4)
        manager.togglePlaybackMode()
        assertTrue(manager.hasNext())
    }

    @Test
    fun `hasPrevious true when at non-zero index`() {
        manager.setBaseQueue(allSongs, currentBaseIndex = 1)
        assertTrue(manager.hasPrevious())
    }

    @Test
    fun `hasPrevious false at index 0 without Infinite`() {
        manager.setBaseQueue(allSongs, currentBaseIndex = 0)
        assertFalse(manager.hasPrevious())
    }

    @Test
    fun `hasPrevious true with Infinite repeat at index 0`() {
        manager.setBaseQueue(allSongs, currentBaseIndex = 0)
        manager.togglePlaybackMode()
        manager.togglePlaybackMode()
        assertTrue(manager.hasPrevious())
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  replaceQueuesPreservingState
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    fun `replaceQueuesPreservingState replaces queues`() {
        manager.setBaseQueue(allSongs)

        val newBase = listOf(song5, song4, song3)
        val newManual = listOf(song1)
        manager.replaceQueuesPreservingState(newBase, newManual, currentBaseIndex = 1)

        val state = manager.queueState.value
        assertEquals(newBase, state.baseQueue)
        assertEquals(newManual, state.manualQueue)
        assertEquals(1, state.currentBaseIndex)
    }

    @Test
    fun `replaceQueuesPreservingState preserves other state fields`() {
        manager.setBaseQueue(allSongs)
        manager.togglePlaybackMode()

        val newBase = listOf(song5, song4, song3)
        manager.replaceQueuesPreservingState(newBase, emptyList(), currentBaseIndex = 0)

        val state = manager.queueState.value
        assertEquals(PlaybackMode.REPEAT, state.playbackMode)
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  restoreState
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    fun `restoreState sets state exactly`() {
        val savedState = QueueState(
            baseQueue = allSongs,
            manualQueue = listOf(song5),
            currentBaseIndex = 2,
            currentManualSong = song4,
            playbackMode = PlaybackMode.REPEAT,
            contextId = "restored-context"
        )

        manager.restoreState(savedState, positionMs = 50000L)

        assertEquals(savedState, manager.queueState.value)
    }

    @Test
    fun `restoreState emits NewQueue with position`() {
        val savedState = QueueState(baseQueue = allSongs)
        manager.restoreState(savedState, positionMs = 50000L)
        drain()

        assertTrue(collectedIntents.isNotEmpty())
        val lastIntent = collectedIntents.last()
        assertTrue(lastIntent is QueueIntent.NewQueue, "Expected NewQueue, got ${lastIntent::class.simpleName}")
        assertEquals(50000L, (lastIntent as QueueIntent.NewQueue).positionMs)
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  Integration: Dual Queue Lifecycle
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    fun `full lifecycle setBaseQueue then playNext through manual back to base`() {
        manager.setBaseQueue(listOf(song1, song2, song3), currentBaseIndex = 0)

        manager.addToManualQueue(song4)
        manager.addToManualQueue(song5)

        manager.playNext()
        assertEquals(song4.title, manager.queueState.value.current?.title)
        assertEquals(1, manager.queueState.value.manualQueue.size)

        manager.playNext()
        assertEquals(song5.title, manager.queueState.value.current?.title)
        assertTrue(manager.queueState.value.manualQueue.isEmpty())

        manager.playNext()
        assertNull(manager.queueState.value.currentManualSong)
        assertEquals(1, manager.queueState.value.currentBaseIndex)
        assertEquals(song2, manager.queueState.value.current)
    }

    @Test
    fun `select song from normal queue skips ahead`() {
        manager.setBaseQueue(allSongs, currentBaseIndex = 0)

        manager.playSongFromQueue(song4.uniqueId)

        val state = manager.queueState.value
        assertEquals(3, state.currentBaseIndex)
        assertEquals(song4, state.current)
        assertEquals(listOf(song1, song2, song3), state.history)
    }

    @Test
    fun `playSongFromQueue with manual song skips preceding manual songs`() {
        manager.setBaseQueue(listOf(song1, song2))
        manager.addToManualQueue(song4)
        manager.addToManualQueue(song5)
        manager.addToManualQueue(song3)
        val song5ManualId = manager.queueState.value.manualQueue[1].uniqueId

        manager.playSongFromQueue(song5ManualId)

        val state = manager.queueState.value
        assertNotNull(state.currentManualSong)
        assertEquals(song5.title, state.currentManualSong!!.title)
        assertEquals(1, state.manualQueue.size)
        assertEquals(song3.title, state.manualQueue[0].title)
    }

    @Test
    fun `auto-advance through manual songs then into base keeps queue in sync`() {
        manager.setBaseQueue(listOf(song1, song2, song3), currentBaseIndex = 0)
        manager.addToManualQueue(song4)
        manager.addToManualQueue(song5)

        manager.playNext(fromAutoAdvanced = true)
        assertNotNull(manager.queueState.value.currentManualSong)
        assertEquals(song4.title, manager.queueState.value.current?.title)
        assertEquals(1, manager.queueState.value.manualQueue.size)

        manager.playNext(fromAutoAdvanced = true)
        assertNotNull(manager.queueState.value.currentManualSong)
        assertEquals(song5.title, manager.queueState.value.current?.title)
        assertTrue(manager.queueState.value.manualQueue.isEmpty())

        manager.playNext(fromAutoAdvanced = true)
        assertNull(manager.queueState.value.currentManualSong)
        assertEquals(1, manager.queueState.value.currentBaseIndex)
        assertEquals(song2, manager.queueState.value.current)

        manager.playNext(fromAutoAdvanced = true)
        assertEquals(2, manager.queueState.value.currentBaseIndex)
    }

    @Test
    fun `select manual then previous then next returns to correct spot`() {
        manager.setBaseQueue(listOf(song1, song2, song3), currentBaseIndex = 1)

        manager.playNext()
        assertEquals(song3, manager.queueState.value.current)
        assertEquals(2, manager.queueState.value.currentBaseIndex)

        manager.playPrevious()
        assertEquals(song2, manager.queueState.value.current)
        assertEquals(1, manager.queueState.value.currentBaseIndex)

        manager.playPrevious()
        assertEquals(song1, manager.queueState.value.current)
        assertEquals(0, manager.queueState.value.currentBaseIndex)

        manager.playNext()
        assertEquals(song2, manager.queueState.value.current)
        assertEquals(1, manager.queueState.value.currentBaseIndex)
    }

    @Test
    fun `add manual queue while manual song is playing queues behind current manual`() {
        manager.setBaseQueue(listOf(song1, song2))
        manager.addToManualQueue(song4)
        manager.playNext()
        assertEquals(song4.title, manager.queueState.value.current?.title)

        manager.addToManualQueue(song5)

        assertEquals(song4.title, manager.queueState.value.currentManualSong!!.title)
        assertEquals(1, manager.queueState.value.manualQueue.size)
        assertEquals(song5.title, manager.queueState.value.manualQueue[0].title)
    }

    @Test
    fun `appendRadioSongs adds only new songs and keeps current index`() {
        manager.setBaseQueue(listOf(song1, song2, song3), currentBaseIndex = 1)

        manager.appendRadioSongs(listOf(song3, song4, song5))

        val state = manager.queueState.value
        assertEquals(listOf(song1, song2, song3, song4, song5), state.baseQueue)
        assertEquals(1, state.currentBaseIndex)
        assertEquals(song2, state.current)
    }

    @Test
    fun `compute playbackQueue after full lifecycle`() {
        manager.setBaseQueue(listOf(song1, song2, song3), currentBaseIndex = 0)
        manager.addToManualQueue(song4)
        manager.addToManualQueue(song5)
        manager.playNext()

        val pq = manager.queueState.value.playbackQueue
        assertEquals(5, pq.size)
        assertEquals(song1, pq[0])
        assertEquals(song4.title, pq[1].title)
        assertEquals(song5.title, pq[2].title)
        assertEquals(song2, pq[3])
        assertEquals(song3, pq[4])
    }
}
