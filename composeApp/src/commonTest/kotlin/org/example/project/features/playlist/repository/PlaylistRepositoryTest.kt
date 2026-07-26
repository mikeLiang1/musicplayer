package org.example.project.features.playlist.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.example.project.core.database.dao.PlaylistDao
import org.example.project.core.database.entity.LikedSongEntity
import org.example.project.core.database.entity.PlaylistEntity
import org.example.project.core.database.entity.PlaylistSongEntity
import org.example.project.core.database.entity.PlaylistWithSongs
import org.example.project.core.database.entity.SongEntity
import org.example.project.core.model.Song
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant

class PlaylistRepositoryTest {

    private lateinit var dao: FakePlaylistDao
    private lateinit var clock: FakeClock
    private lateinit var repository: PlaylistRepository

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
        dao = FakePlaylistDao()
        clock = FakeClock(1000L)
        repository = PlaylistRepository(dao, clock)
    }

    // ── toggleLike ────────────────────────────────────────────────────────────

    @Test
    fun toggleLike_onNewSong_insertsAndMarksLiked() = runBlocking {
        val song1 = song("song1")
        repository.toggleLike(song1)

        assertTrue(repository.isSongLiked(song1.url))
        assertEquals(listOf(song1.url), dao.songs.value.values.map { it.url })
    }

    @Test
    fun toggleLike_twice_unlikesAgain() = runBlocking {
        val song1 = song("song1")
        repository.toggleLike(song1)
        repository.toggleLike(song1)

        assertFalse(repository.isSongLiked(song1.url))
        // The song row itself is not removed from the library, only unmarked.
        assertTrue(dao.songs.value.containsKey(song1.url))
    }

    @Test
    fun toggleLike_doesNotAffectOtherSongs() = runBlocking {
        val song1 = song("song1")
        val song2 = song("song2")
        repository.toggleLike(song1)
        repository.toggleLike(song2)
        repository.toggleLike(song1) // unlike song1 only

        assertFalse(repository.isSongLiked(song1.url))
        assertTrue(repository.isSongLiked(song2.url))
    }

    @Test
    fun toggleLike_onSongAlreadyInLibrary_doesNotOverwriteExistingMetadata() = runBlocking {
        val song1 = song("song1")
        val original = song1.toSongEntityForTest(firstAddedAt = 42L)
        dao.songs.value = mapOf(original.url to original)

        clock.now = 9999L
        repository.toggleLike(song1)

        // insertSongIfMissing must not clobber the row already present.
        assertEquals(42L, dao.songs.value[song1.url]?.firstAddedAt)
        assertTrue(repository.isSongLiked(song1.url))
    }

    // ── getLikedSongs / getLikedSongCount ────────────────────────────────────

    @Test
    fun getLikedSongs_ordersNewestFirst() = runBlocking {
        val song1 = song("song1")
        val song2 = song("song2")
        val song3 = song("song3")

        clock.now = 1L
        repository.toggleLike(song1)
        clock.now = 3L
        repository.toggleLike(song3)
        clock.now = 2L
        repository.toggleLike(song2)

        val liked = repository.getLikedSongs().first()
        assertEquals(listOf(song3.url, song2.url, song1.url), liked.map { it.url })
    }

    @Test
    fun getLikedSongs_ordersByLikedAt_notByWhenSongEnteredLibrary() = runBlocking {
        // song1 has been in the library (e.g. in a playlist) since long ago.
        val song1 = song("song1")
        dao.songs.value = mapOf(song1.url to song1.toSongEntityForTest(firstAddedAt = 1L))

        val song2 = song("song2")
        clock.now = 100L
        repository.toggleLike(song2)
        clock.now = 200L
        repository.toggleLike(song1) // liked most recently → must come first

        val liked = repository.getLikedSongs().first()
        assertEquals(listOf(song1.url, song2.url), liked.map { it.url })
    }

    @Test
    fun getLikedSongs_excludesUnlikedSongs() = runBlocking {
        val song1 = song("song1")
        val song2 = song("song2")
        repository.toggleLike(song1)
        repository.toggleLike(song2)
        repository.toggleLike(song2) // unlike

        val liked = repository.getLikedSongs().first()
        assertEquals(listOf(song1.url), liked.map { it.url })
    }

    @Test
    fun getLikedSongCount_reflectsLikeAndUnlike() = runBlocking {
        val song1 = song("song1")
        val song2 = song("song2")

        assertEquals(0, repository.getLikedSongCount().first())

        repository.toggleLike(song1)
        repository.toggleLike(song2)
        assertEquals(2, repository.getLikedSongCount().first())

        repository.toggleLike(song1)
        assertEquals(1, repository.getLikedSongCount().first())
    }

    // ── likeSong / unlikeSong ─────────────────────────────────────────────────

    @Test
    fun likeSong_twice_staysLikedAndKeepsOriginalLikedAt() = runBlocking {
        val song1 = song("song1")
        clock.now = 100L
        repository.likeSong(song1)
        clock.now = 200L
        repository.likeSong(song1) // the player heart may fire again before state settles

        assertTrue(repository.isSongLiked(song1.url))
        assertEquals(1, repository.getLikedSongCount().first())
        assertEquals(100L, dao.likedSongs.value[song1.url]?.likedAt)
    }

    @Test
    fun unlikeSong_removesOnlyTheLikeMark() = runBlocking {
        val song1 = song("song1")
        repository.likeSong(song1)
        repository.unlikeSong(song1.url)

        assertFalse(repository.isSongLiked(song1.url))
        assertTrue(dao.songs.value.containsKey(song1.url))
    }

    // ── isSongLiked / observeIsSongLiked ──────────────────────────────────────

    @Test
    fun isSongLiked_onUnknownSong_isFalse() = runBlocking {
        assertFalse(repository.isSongLiked("url-never-seen"))
    }

    @Test
    fun observeIsSongLiked_emitsAcrossLikeAndUnlike() = runBlocking {
        val song1 = song("song1")
        val liked = repository.observeIsSongLiked(song1.url)

        assertFalse(liked.first())

        repository.likeSong(song1)
        assertTrue(liked.first())

        repository.unlikeSong(song1.url)
        assertFalse(liked.first())
    }

    @Test
    fun observeIsSongLiked_isScopedToItsOwnSong() = runBlocking {
        val song1 = song("song1")
        val song2 = song("song2")
        repository.likeSong(song2)

        assertFalse(repository.observeIsSongLiked(song1.url).first())
        assertTrue(repository.observeIsSongLiked(song2.url).first())
    }

    // ── lastPlayedAt / updatedAt ──────────────────────────────────────────────
    // The library sort key is lastPlayedAt (falling back to createdAt); updatedAt is metadata
    // that records content edits only. Keeping the two apart is what stops rows from
    // reordering under the user while the add-to-playlist sheet is open.

    @Test
    fun markPlayed_setsLastPlayedAt_andLeavesUpdatedAtAlone() = runBlocking {
        val playlist = repository.createPlaylist("Road trip")
        val createdUpdatedAt = dao.playlists.single().updatedAt

        clock.now = 5000L
        repository.markPlayed(playlist.id)

        val stored = dao.playlists.single()
        assertEquals(5000L, stored.lastPlayedAt)
        assertEquals(createdUpdatedAt, stored.updatedAt)
    }

    @Test
    fun addSong_doesNotChangeLastPlayedAt() = runBlocking {
        val playlist = repository.createPlaylist("Road trip")

        clock.now = 5000L
        repository.addSong(playlist.id, song("song1"))

        val stored = dao.playlists.single()
        // The sort key must be untouched by an edit, or the row jumps to the top of the sheet.
        assertEquals(0L, stored.lastPlayedAt)
        assertEquals(5000L, stored.updatedAt)
    }

    @Test
    fun newPlaylist_hasNeverBeenPlayed() = runBlocking {
        repository.createPlaylist("Road trip")

        // 0 is what makes the query fall back to createdAt, so a new playlist sorts to the top
        // instead of below every playlist that has ever been played.
        assertEquals(0L, dao.playlists.single().lastPlayedAt)
    }

    @Test
    fun removePlaylistSong_marksThePlaylistEdited() = runBlocking {
        val playlist = repository.createPlaylist("Road trip")
        repository.addSong(playlist.id, song("song1"))
        val playlistSongId = dao.playlistSongs.single().id

        clock.now = 9000L
        repository.removePlaylistSong(playlistSongId)

        // Removing is as much an edit as adding — both bump updatedAt.
        assertEquals(9000L, dao.playlists.single().updatedAt)
        assertTrue(dao.playlistSongs.isEmpty())
    }
}

private class FakeClock(var now: Long) : Clock {
    override fun now(): Instant = Instant.fromEpochMilliseconds(now)
}

private fun Song.toSongEntityForTest(firstAddedAt: Long) = SongEntity(
    url = url,
    title = title,
    artist = artist,
    thumbnailUrl = thumbnailUrl,
    duration = duration,
    firstAddedAt = firstAddedAt
)

/**
 * In-memory fake of [PlaylistDao]. Only the "songs" and "liked_songs" tables are reactive (via
 * StateFlows) since that's what the liked-songs Flows depend on; playlist/junction rows are
 * plain lists as no current test exercises their reactivity.
 */
private class FakePlaylistDao : PlaylistDao {
    val songs = MutableStateFlow<Map<String, SongEntity>>(emptyMap())
    val likedSongs = MutableStateFlow<Map<String, LikedSongEntity>>(emptyMap())
    val playlists = mutableListOf<PlaylistEntity>()
    val playlistSongs = mutableListOf<PlaylistSongEntity>()

    override fun getAllPlaylists(): Flow<List<PlaylistEntity>> = MutableStateFlow(playlists.toList())

    override fun getAllPlaylistsWithSongs(): Flow<List<PlaylistWithSongs>> =
        MutableStateFlow(emptyList())

    override fun getPlaylistWithSongs(playlistId: String): Flow<PlaylistWithSongs?> =
        MutableStateFlow(null)

    override suspend fun insertPlaylist(playlist: PlaylistEntity) {
        playlists.add(playlist)
    }

    override suspend fun renamePlaylist(id: String, name: String, timestamp: Long) {
        val index = playlists.indexOfFirst { it.id == id }
        if (index >= 0) playlists[index] = playlists[index].copy(name = name, updatedAt = timestamp)
    }

    override suspend fun touchPlaylist(id: String, timestamp: Long) {
        val index = playlists.indexOfFirst { it.id == id }
        if (index >= 0) playlists[index] = playlists[index].copy(updatedAt = timestamp)
    }

    override suspend fun markPlaylistPlayed(id: String, timestamp: Long) {
        val index = playlists.indexOfFirst { it.id == id }
        if (index >= 0) playlists[index] = playlists[index].copy(lastPlayedAt = timestamp)
    }

    override suspend fun deletePlaylist(id: String) {
        playlists.removeAll { it.id == id }
    }

    override suspend fun insertSongIfMissing(song: SongEntity) {
        // OnConflictStrategy.IGNORE semantics: never overwrite an existing row.
        if (!songs.value.containsKey(song.url)) {
            songs.value = songs.value + (song.url to song)
        }
    }

    override suspend fun getSong(url: String): SongEntity? = songs.value[url]

    override suspend fun isSongLiked(url: String): Boolean = likedSongs.value.containsKey(url)

    override fun observeIsSongLiked(url: String): Flow<Boolean> =
        likedSongs.map { it.containsKey(url) }

    override suspend fun insertLikedSong(likedSong: LikedSongEntity) {
        // OnConflictStrategy.IGNORE semantics: never overwrite an existing row.
        if (!likedSongs.value.containsKey(likedSong.songUrl)) {
            likedSongs.value = likedSongs.value + (likedSong.songUrl to likedSong)
        }
    }

    override suspend fun deleteLikedSong(url: String) {
        likedSongs.value = likedSongs.value - url
    }

    override fun getLikedSongs(): Flow<List<SongEntity>> =
        combine(songs, likedSongs) { songMap, likedMap ->
            likedMap.values
                .sortedByDescending { it.likedAt }
                .mapNotNull { songMap[it.songUrl] }
        }

    override fun getLikedSongCount(): Flow<Int> = likedSongs.map { it.size }

    override suspend fun insertPlaylistSong(playlistSong: PlaylistSongEntity) {
        playlistSongs.add(playlistSong)
    }

    override suspend fun insertPlaylistSongs(playlistSongs: List<PlaylistSongEntity>) {
        this.playlistSongs.addAll(playlistSongs)
    }

    override suspend fun deletePlaylistSong(id: String) {
        playlistSongs.removeAll { it.id == id }
    }

    override suspend fun touchPlaylistOwning(playlistSongId: String, timestamp: Long) {
        val owner = playlistSongs.firstOrNull { it.id == playlistSongId }?.playlistId ?: return
        touchPlaylist(owner, timestamp)
    }

    override suspend fun deleteAllPlaylistSongs(playlistId: String) {
        playlistSongs.removeAll { it.playlistId == playlistId }
    }

    override suspend fun getNextPosition(playlistId: String): Int =
        (playlistSongs.filter { it.playlistId == playlistId }.maxOfOrNull { it.position } ?: -1) + 1
}
