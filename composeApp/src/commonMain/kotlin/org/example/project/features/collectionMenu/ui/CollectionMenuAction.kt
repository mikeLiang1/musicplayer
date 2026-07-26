package org.example.project.features.collectionMenu.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Queue
import org.example.project.core.model.Song
import org.example.project.ui.component.MenuAccent
import org.example.project.ui.component.MenuAction

/**
 * Rows of the collection-level menu (the ⋮ in `SongCollectionHeader`) — actions on a whole
 * collection of songs, as opposed to `SongMenuAction`, which acts on one song.
 *
 * Play and shuffle deliberately aren't here: the header already exposes both as buttons.
 */
sealed class CollectionMenuAction : MenuAction {

    /** Appends every song in the collection to the manual queue, in the collection's order. */
    data object AddToQueue : CollectionMenuAction() {
        override val label = "Add all to queue"
        override val icon = Icons.Rounded.Queue
    }

    data object Rename : CollectionMenuAction() {
        override val label = "Rename playlist"
        override val icon = Icons.Rounded.Edit
    }

    data object Delete : CollectionMenuAction() {
        override val label = "Delete playlist"
        override val icon = Icons.Rounded.DeleteOutline
        override val accent = MenuAccent.Destructive
    }
}

/**
 * What the menu is acting on — a snapshot taken when the sheet opens, the way
 * `SongMenuViewModel` snapshots the selected `Song`.
 *
 * The subtype decides which rows appear: only a user playlist can be renamed or deleted.
 */
sealed interface CollectionMenuTarget {
    val title: String
    val songs: List<Song>

    /** Which rows this target offers. [AddToQueue] is dropped when there is nothing to queue. */
    val actions: List<CollectionMenuAction>

    /** A user-created playlist — the full menu. */
    data class UserPlaylist(
        val playlistId: String,
        override val title: String,
        override val songs: List<Song>
    ) : CollectionMenuTarget {
        override val actions: List<CollectionMenuAction>
            get() = buildList {
                if (songs.isNotEmpty()) add(CollectionMenuAction.AddToQueue)
                add(CollectionMenuAction.Rename)
                add(CollectionMenuAction.Delete)
            }
    }

    /**
     * A system collection such as Liked Songs: it can't be renamed or deleted, so the menu is
     * queue-only — and empty when the collection is, which the caller uses to hide the ⋮.
     */
    data class SystemCollection(
        override val title: String,
        override val songs: List<Song>
    ) : CollectionMenuTarget {
        override val actions: List<CollectionMenuAction>
            get() = if (songs.isEmpty()) emptyList() else listOf(CollectionMenuAction.AddToQueue)
    }
}
