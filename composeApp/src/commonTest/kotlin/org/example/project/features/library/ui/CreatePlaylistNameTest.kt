package org.example.project.features.library.ui

import kotlin.test.Test
import kotlin.test.assertEquals

class CreatePlaylistNameTest {

    @Test
    fun `suggests the base name when nothing exists`() {
        assertEquals("My playlist", suggestDefaultPlaylistName(emptyList()))
    }

    @Test
    fun `ignores unrelated names`() {
        assertEquals("My playlist", suggestDefaultPlaylistName(listOf("Road trip", "Focus")))
    }

    @Test
    fun `appends 2 when the base name is taken`() {
        assertEquals("My playlist 2", suggestDefaultPlaylistName(listOf("My playlist")))
    }

    @Test
    fun `walks up to the first free suffix`() {
        assertEquals(
            "My playlist 3",
            suggestDefaultPlaylistName(listOf("My playlist", "My playlist 2"))
        )
    }

    @Test
    fun `fills a gap in the middle of the sequence`() {
        assertEquals(
            "My playlist 2",
            suggestDefaultPlaylistName(listOf("My playlist", "My playlist 3"))
        )
    }

    @Test
    fun `matches ignoring case and surrounding whitespace`() {
        assertEquals(
            "My playlist 2",
            suggestDefaultPlaylistName(listOf("  my PLAYLIST "))
        )
    }
}
