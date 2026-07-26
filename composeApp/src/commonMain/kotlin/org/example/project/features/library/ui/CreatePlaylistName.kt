package org.example.project.features.library.ui

private const val DEFAULT_PLAYLIST_NAME = "My playlist"

/**
 * Suggests the name pre-filled into the create-playlist sheet: [DEFAULT_PLAYLIST_NAME], or the
 * first free "$DEFAULT_PLAYLIST_NAME N" if that is taken. Matching is trimmed and case-insensitive
 * so "my playlist " counts as a collision — the point is to avoid a list of look-alike rows, and
 * the user can always type over the suggestion anyway.
 */
internal fun suggestDefaultPlaylistName(existingNames: List<String>): String {
    val taken = existingNames.map { it.trim().lowercase() }.toSet()
    if (DEFAULT_PLAYLIST_NAME.lowercase() !in taken) return DEFAULT_PLAYLIST_NAME

    var suffix = 2
    while ("$DEFAULT_PLAYLIST_NAME $suffix".lowercase() in taken) {
        suffix++
    }
    return "$DEFAULT_PLAYLIST_NAME $suffix"
}
