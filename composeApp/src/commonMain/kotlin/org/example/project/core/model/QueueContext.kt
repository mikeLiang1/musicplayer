package org.example.project.core.model

/**
 * Where the current queue came from. Two jobs:
 *  - identity ([id]) — screens compare it against their own id to know whether *they* are what's
 *    playing (playlist/liked-songs "is this collection active?" checks).
 *  - display ([type] + [title]) — the "PLAYING FROM …" label in the full-screen player header.
 *
 * A null context means the queue has no meaningful source (nothing played yet, or a queue
 * restored from a build that predates this being persisted).
 */
data class QueueContext(
    val id: String,
    val type: QueueContextType,
    val title: String
)

enum class QueueContextType {
    /** InnerTube radio generated from a seed song; [QueueContext.title] is the seed song's title. */
    RADIO,
    PLAYLIST,
    LIKED_SONGS;

    companion object {
        /** Persistence-safe parse: unknown/removed names restore as null rather than crashing. */
        fun fromName(name: String?): QueueContextType? = entries.firstOrNull { it.name == name }
    }
}
