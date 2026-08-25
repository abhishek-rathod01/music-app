package com.abhishekrathod.musicapp.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class QueueItemTest {
    @Test
    fun `rejects blank id`() {
        assertThrows(IllegalArgumentException::class.java) { QueueItemId("") }
    }

    @Test
    fun `two items wrapping the same track are distinct occurrences`() {
        val track = TrackId("t1")
        val first = QueueItem(QueueItemId("q1"), track)
        val second = QueueItem(QueueItemId("q2"), track)

        // Same track, different occurrence — this is the whole point of
        // QueueItemId existing separately from TrackId. See its KDoc.
        assertNotEquals(first, second)
        assertEquals(first.trackId, second.trackId)
    }
}
