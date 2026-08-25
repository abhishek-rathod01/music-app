package com.abhishekrathod.musicapp.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class PlaylistTest {
    @Test
    fun `rejects blank name`() {
        assertThrows(IllegalArgumentException::class.java) {
            Playlist(PlaylistId("p1"), "", emptyList())
        }
    }

    @Test
    fun `allows an empty track list`() {
        val playlist = Playlist(PlaylistId("p1"), "Empty", emptyList())
        assertEquals(emptyList<TrackId>(), playlist.trackIds)
    }

    @Test
    fun `preserves track order and duplicates`() {
        val a = TrackId("a")
        val b = TrackId("b")
        val playlist = Playlist(PlaylistId("p1"), "Repeats", listOf(a, b, a))
        assertEquals(listOf(a, b, a), playlist.trackIds)
    }
}
