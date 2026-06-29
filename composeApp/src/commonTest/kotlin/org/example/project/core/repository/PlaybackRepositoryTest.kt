package org.example.project.core.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.example.project.core.database.dao.PlaybackDao
import org.example.project.core.database.entity.PlaybackStateEntity
import org.example.project.core.database.entity.QueueEntity
import org.example.project.core.manager.PlaybackMode
import org.example.project.core.manager.QueueState
import org.example.project.core.model.Song
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PlaybackRepositoryTest {

    private lateinit var dao: FakePlaybackDao
    private lateinit var repository: PlaybackRepository

    // ── Fixtures ───────────────────────────────────────────────────────────────
    private val song1 = song("song1")
    private val song2 = song("song2")
    private val song3 = song("song3")
    private val manual1 = song("manual1")
    private val manual2 = song("manual2")

    private fun song(id: String) = Song(
        uniqueId = id,
        url = "url-$id",
        title = "Title $id",
        artist = "Artist $id",
        thumbnailUrl = "thumb-$id",
        duration = 1000L
    )

    @BeforeTest
    fun setup() {
        dao = FakePlaybackDao()
        // Unconfined dispatcher makes the init seeding run synchronously during construction.
        repository = PlaybackRepository(dao, Dispatchers.Unconfined)
    }

    // ── Initialization ─────────────────────────────────────────────────────────

    @Test
    fun init_seedsDefaultPlaybackStateRow() {
        // The init block should have inserted the id=0 row.
        assertEquals(PlaybackStateEntity(id = 0), dao.state)
    }

    @Test
    fun init_doesNotOverwriteExistingState() = runBlocking {
        val preexisting = FakePlaybackDao().apply {
            state = PlaybackStateEntity(id = 0, positionMs = 9999L)
        }
        PlaybackRepository(preexisting, Dispatchers.Unconfined)
        assertEquals(9999L, preexisting.state?.positionMs)
    }

    // ── Position ────────────────────────────────────────────────────────────────

    @Test
    fun savePosition_isRestored() = runBlocking {
        repository.savePosition(42_000L)

        val restored = repository.getRestoredPlayback()
        assertEquals(42_000L, restored?.positionMs)
    }

    // ── Round-trip of queue state ────────────────────────────────────────────────

    @Test
    fun saveAndRestore_baseAndManualQueuesInOrder() = runBlocking {
        val state = QueueState(
            baseQueue = listOf(song1, song2, song3),
            manualQueue = listOf(manual1, manual2),
            currentBaseIndex = 1,
            playbackMode = PlaybackMode.REPEAT
        )
        repository.saveQueueState(state)

        val restored = repository.getRestoredPlayback()!!.queueState
        assertEquals(listOf("song1", "song2", "song3"), restored.baseQueue.map { it.uniqueId })
        assertEquals(listOf("manual1", "manual2"), restored.manualQueue.map { it.uniqueId })
        assertEquals(1, restored.currentBaseIndex)
        assertEquals(PlaybackMode.REPEAT, restored.playbackMode)
        assertEquals(false, restored.isShuffled)
        assertNull(restored.currentManualSong)
        assertNull(restored.preShuffleBaseQueue)
    }

    @Test
    fun saveAndRestore_preservesSongFields() = runBlocking {
        repository.saveQueueState(QueueState(baseQueue = listOf(song1)))

        val restored = repository.getRestoredPlayback()!!.queueState.baseQueue.single()
        assertEquals(song1.uniqueId, restored.uniqueId)
        assertEquals(song1.url, restored.url)
        assertEquals(song1.title, restored.title)
        assertEquals(song1.artist, restored.artist)
        assertEquals(song1.thumbnailUrl, restored.thumbnailUrl)
        assertEquals(song1.duration, restored.duration)
    }

    @Test
    fun saveAndRestore_currentManualSong() = runBlocking {
        val state = QueueState(
            baseQueue = listOf(song1, song2),
            manualQueue = listOf(manual2),
            currentBaseIndex = 0,
            currentManualSong = manual1
        )
        repository.saveQueueState(state)

        val restored = repository.getRestoredPlayback()!!.queueState
        assertEquals("manual1", restored.currentManualSong?.uniqueId)
        // current resolves to the manual song when one is playing
        assertEquals("manual1", restored.current?.uniqueId)
    }

    // ── Shuffle snapshot (#3 regression) ─────────────────────────────────────────

    @Test
    fun saveAndRestore_shuffleSnapshot_preShuffleIndexIsNull() = runBlocking {
        val state = QueueState(
            baseQueue = listOf(song2, song1, song3), // "shuffled" order
            currentBaseIndex = 2,
            isShuffled = true,
            preShuffleBaseQueue = listOf(song1, song2, song3) // original order
        )
        repository.saveQueueState(state)

        val restored = repository.getRestoredPlayback()!!.queueState
        assertTrue(restored.isShuffled)
        assertEquals(listOf("song1", "song2", "song3"), restored.preShuffleBaseQueue?.map { it.uniqueId })
        // Regression: the shuffled index must NOT leak into preShuffleBaseIndex.
        assertNull(restored.preShuffleBaseIndex)
    }

    @Test
    fun restore_noSnapshot_yieldsNullPreShuffleQueue() = runBlocking {
        repository.saveQueueState(QueueState(baseQueue = listOf(song1), isShuffled = false))

        val restored = repository.getRestoredPlayback()!!.queueState
        assertNull(restored.preShuffleBaseQueue)
    }

    // ── Backward-compat fallback for currentManualSong by id ─────────────────────

    @Test
    fun restore_currentManualSong_fallsBackToIdLookup() = runBlocking {
        // Simulate legacy data: a currentManualSongId pointer with no "current_manual" row,
        // where the referenced song lives in the manual queue.
        dao.queueRows.add(manual1.toRow(type = "manual", order = 0))
        dao.state = PlaybackStateEntity(id = 0, currentManualSongId = "manual1")

        val restored = repository.getRestoredPlayback()!!.queueState
        assertEquals("manual1", restored.currentManualSong?.uniqueId)
    }

    // ── Rewrite-skipping optimization (A) ────────────────────────────────────────

    @Test
    fun saveQueueState_skipsRowRewriteWhenContentsUnchanged() = runBlocking {
        val state = QueueState(baseQueue = listOf(song1, song2), currentBaseIndex = 0)
        repository.saveQueueState(state)
        val clearsAfterFirst = dao.clearCount

        // Same contents, only the index advances → no row rewrite, but state still updated.
        repository.saveQueueState(state.copy(currentBaseIndex = 1))

        assertEquals(clearsAfterFirst, dao.clearCount)
        assertEquals(1, dao.state?.currentIndex)
    }

    @Test
    fun saveQueueState_rewritesRowsWhenContentsChange() = runBlocking {
        repository.saveQueueState(QueueState(baseQueue = listOf(song1, song2)))
        val clearsAfterFirst = dao.clearCount

        repository.saveQueueState(QueueState(baseQueue = listOf(song1, song2, song3)))

        assertEquals(clearsAfterFirst + 1, dao.clearCount)
        val restored = repository.getRestoredPlayback()!!.queueState
        assertEquals(listOf("song1", "song2", "song3"), restored.baseQueue.map { it.uniqueId })
    }

    @Test
    fun restore_primesSignature_soFollowingIdenticalSaveSkipsRewrite() = runBlocking {
        repository.saveQueueState(QueueState(baseQueue = listOf(song1, song2), currentBaseIndex = 0))

        val restored = repository.getRestoredPlayback()!!.queueState
        val clearsBefore = dao.clearCount

        // Saving the just-restored state (same contents) should not rewrite rows.
        repository.saveQueueState(restored.copy(currentBaseIndex = 1))

        assertEquals(clearsBefore, dao.clearCount)
    }

    @Test
    fun saveQueueState_shuffleChangeAloneDoesNotRewriteRows() = runBlocking {
        val base = listOf(song1, song2, song3)
        repository.saveQueueState(QueueState(baseQueue = base, currentBaseIndex = 0))
        val clearsBefore = dao.clearCount

        // Toggling shuffle/repeat flags without changing the song list is a pointer-only change.
        repository.saveQueueState(
            QueueState(baseQueue = base, currentBaseIndex = 0, playbackMode = PlaybackMode.Infinite)
        )

        assertEquals(clearsBefore, dao.clearCount)
        assertEquals(PlaybackMode.Infinite.name, dao.state?.repeatMode)
    }
}

/**
 * In-memory fake of [PlaybackDao]. `saveAllQueues` uses the interface's default body
 * (clearAllQueues + insertQueue), so clearCount tracks how often the rows are rewritten.
 */
private class FakePlaybackDao : PlaybackDao {
    var state: PlaybackStateEntity? = null
    val queueRows = mutableListOf<QueueEntity>()
    var clearCount = 0
    var insertCount = 0
    private var autoId = 1

    private val queueTypes = setOf("base", "manual", "shuffle_snapshot", "current_manual")

    override suspend fun insertQueue(songs: List<QueueEntity>) {
        insertCount++
        songs.forEach { queueRows.add(it.copy(autoId = autoId++)) }
    }

    override suspend fun getPlaybackStateOnce(): PlaybackStateEntity? = state

    override suspend fun upsertPlaybackState(state: PlaybackStateEntity) {
        this.state = state
    }

    override suspend fun updatePosition(position: Long) {
        state = (state ?: PlaybackStateEntity(id = 0)).copy(positionMs = position)
    }

    override suspend fun clearAllQueues() {
        clearCount++
        queueRows.removeAll { it.type in queueTypes }
    }

    override suspend fun getQueueByType(type: String): List<QueueEntity> =
        queueRows.filter { it.type == type }.sortedBy { it.orderIndex }

    override suspend fun updatePlaybackState(
        currentIndex: Int?,
        isShuffled: Boolean,
        repeatMode: String?,
        currentManualSongId: String?
    ) {
        state = (state ?: PlaybackStateEntity(id = 0)).copy(
            currentIndex = currentIndex,
            isShuffled = isShuffled,
            repeatMode = repeatMode,
            currentManualSongId = currentManualSongId
        )
    }
}

private fun Song.toRow(type: String, order: Int) = QueueEntity(
    uniqueId = uniqueId,
    title = title,
    artist = artist,
    thumbnailUrl = thumbnailUrl,
    url = url,
    duration = duration,
    type = type,
    isManual = false,
    orderIndex = order
)
