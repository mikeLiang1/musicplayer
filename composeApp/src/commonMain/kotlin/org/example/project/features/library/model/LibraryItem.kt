package org.example.project.features.library.model

import org.example.project.core.model.Playlist

sealed interface LibraryItem {
    data class PlaylistItem(val playlist: Playlist) : LibraryItem

}

fun LibraryItem.stableKey(): String = when (this) {
    is LibraryItem.PlaylistItem -> "artist_${playlist.id}"
}
