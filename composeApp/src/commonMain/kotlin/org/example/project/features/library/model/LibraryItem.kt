package org.example.project.features.library.model

import org.example.project.core.model.Playlist
import org.example.project.core.model.Song

sealed class LibraryItem {
    data class SongItem(val song: Song) : LibraryItem()
    data class PlaylistItem(val playlist: Playlist) : LibraryItem()


}

fun LibraryItem.stableKey(): String = when (this) {
    is LibraryItem.SongItem -> "song_${song.uniqueId}"
    is LibraryItem.PlaylistItem -> "artist_${playlist.uniqueId}"
}
