package org.example.project.features.musicPlayer.ui

import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import kotlinx.coroutines.flow.filter
import org.example.project.core.model.Song
import org.example.project.features.musicPlayer.model.PlayerQueue
import kotlin.math.abs

/**
 * Swipe-to-change-song carousel shared by [SongScreen]'s artwork and the [MusicPlayerBar].
 *
 * One page per song in [queue]'s `allSongs` (history + current + manual + upcoming). The
 * [HorizontalPager] owns the gesture, fling and snap; we only bridge its position to the queue
 * in both directions, tracking position by **song identity** ([Song.uniqueId]) rather than by
 * index — committing a swipe can restructure the queue (e.g. selecting a manual song drops the
 * ones it skipped), so an index isn't stable across a commit.
 *
 *  - User swipe → on the pager coming to **rest**, commit the settled song via [onSongSelected].
 *    Committing only at rest (never mid-fling) means the queue can't restructure while the pager
 *    is still moving, and the page index + song list are read at the same instant so they can't
 *    drift apart.
 *  - Current song changed elsewhere (button, auto-advance, queue pick, the other pager) → move
 *    the pager to it: a one-step move animates, a bigger jump snaps (avoids lag scrolling past
 *    every page). Skipped when the page already shows the current song, so a swipe — or the
 *    reindex its own commit triggers — is a no-op rather than a fight.
 */
@Composable
fun SongPager(
    queue: PlayerQueue,
    onSongSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    pageContent: @Composable (Song) -> Unit,
) {
    val songs = queue.allSongs
    val current = queue.current ?: return
    if (songs.isEmpty()) return

    val pagerState = rememberPagerState(
        initialPage = queue.absoluteIndexOf(current).coerceAtLeast(0),
    ) { songs.size }

    // Current song changed elsewhere → move the pager to it (by identity, not index). Skip if the
    // page already shows it (the user's own swipe, or a post-commit reindex) or the user is
    // scrolling, so we never fight the finger or bounce on a reindex.
    LaunchedEffect(current.uniqueId) {
        if (pagerState.isScrollInProgress) return@LaunchedEffect
        if (songs.getOrNull(pagerState.currentPage)?.uniqueId == current.uniqueId) return@LaunchedEffect
        val target = songs.indexOfFirst { it.uniqueId == current.uniqueId }
        if (target < 0) return@LaunchedEffect
        if (abs(target - pagerState.currentPage) == 1) pagerState.animateScrollToPage(target)
        else pagerState.scrollToPage(target)
    }

    // User swipe → commit the settled song once the pager comes to rest. The collector outlives
    // queue changes, so read the latest queue (not the captured one), and resolve the page index
    // against it at this same instant.
    val latestQueue by rememberUpdatedState(queue)
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.isScrollInProgress }
            .filter { !it }
            .collect {
                val q = latestQueue
                val song = q.allSongs.getOrNull(pagerState.currentPage) ?: return@collect
                if (song.uniqueId != q.current?.uniqueId) onSongSelected(song.uniqueId)
            }
    }

    HorizontalPager(
        state = pagerState,
        key = { songs[it].uniqueId },
        modifier = modifier,
    ) { page -> pageContent(songs[page]) }
}
