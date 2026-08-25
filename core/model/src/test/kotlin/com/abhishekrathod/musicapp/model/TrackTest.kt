package com.abhishekrathod.musicapp.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class TrackIdTest {
    @Test
    fun `rejects blank value`() {
        assertThrows(IllegalArgumentException::class.java) { TrackId("") }
        assertThrows(IllegalArgumentException::class.java) { TrackId("   ") }
    }

    @Test
    fun `equal values are equal ids`() {
        assertEquals(TrackId("abc"), TrackId("abc"))
    }
}

class TrackTest {
    private fun track(
        id: String = "t1",
        title: String = "Song",
        artist: String = "Artist",
        durationMs: Long? = 1000L,
        artworkUrl: String? = null,
    ) = Track(TrackId(id), title, artist, durationMs, artworkUrl)

    @Test
    fun `rejects blank title`() {
        assertThrows(IllegalArgumentException::class.java) { track(title = "") }
    }

    @Test
    fun `rejects negative duration`() {
        assertThrows(IllegalArgumentException::class.java) { track(durationMs = -1L) }
    }

    @Test
    fun `allows null duration for incomplete metadata`() {
        val t = track(durationMs = null)
        assertEquals(null, t.durationMs)
    }

    @Test
    fun `allows zero duration explicitly`() {
        // Zero is a valid (if unusual) known duration, distinct from null
        // ("unknown"). See the KDoc on Track.durationMs.
        val t = track(durationMs = 0L)
        assertEquals(0L, t.durationMs)
    }
}
