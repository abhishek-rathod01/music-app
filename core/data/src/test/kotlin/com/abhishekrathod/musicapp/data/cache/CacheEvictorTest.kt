package com.abhishekrathod.musicapp.data.cache

import com.abhishekrathod.musicapp.model.TrackId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private fun entry(id: String, sizeBytes: Long, lastAccessedAtMs: Long) =
    CacheEntry(TrackId(id), sizeBytes, lastAccessedAtMs)

class CacheEvictorTest {
    @Test
    fun `under the cap evicts nothing`() {
        val entries = listOf(entry("a", 100, 1), entry("b", 100, 2))

        assertEquals(emptyList<CacheEntry>(), CacheEvictor.evictionsFor(entries, capBytes = 500, currentlyPlaying = null))
    }

    @Test
    fun `empty cache evicts nothing`() {
        assertEquals(
            emptyList<CacheEntry>(),
            CacheEvictor.evictionsFor(emptyList(), capBytes = 100, currentlyPlaying = null),
        )
    }

    @Test
    fun `evicts the least-recently-accessed entries first`() {
        val oldest = entry("a", 100, lastAccessedAtMs = 1)
        val middle = entry("b", 100, lastAccessedAtMs = 2)
        val newest = entry("c", 100, lastAccessedAtMs = 3)

        // Cap of 150 with 300 total needs 150 freed: evicting `oldest` alone
        // only frees 100, so `middle` (next-oldest) must go too.
        val result = CacheEvictor.evictionsFor(listOf(newest, oldest, middle), capBytes = 150, currentlyPlaying = null)

        assertEquals(listOf(oldest, middle), result)
    }

    @Test
    fun `stops evicting as soon as it is under the cap`() {
        val a = entry("a", 100, 1)
        val b = entry("b", 100, 2)
        val c = entry("c", 100, 3)

        val result = CacheEvictor.evictionsFor(listOf(a, b, c), capBytes = 250, currentlyPlaying = null)

        assertEquals(listOf(a), result)
    }

    @Test
    fun `never evicts the currently playing track even though it is the oldest`() {
        val playing = entry("a", 100, lastAccessedAtMs = 1) // oldest — would normally go first
        val b = entry("b", 100, lastAccessedAtMs = 2)
        val c = entry("c", 100, lastAccessedAtMs = 3)

        val result = CacheEvictor.evictionsFor(
            listOf(playing, b, c),
            capBytes = 100,
            currentlyPlaying = TrackId("a"),
        )

        assertTrue(result.none { it.trackId == TrackId("a") })
        assertEquals(listOf(b, c), result)
    }

    @Test
    fun `cap smaller than the pinned entry alone still never evicts it`() {
        val playing = entry("a", 1000, lastAccessedAtMs = 1)
        val b = entry("b", 50, lastAccessedAtMs = 2)

        // Cap (100) is smaller than the pinned entry (1000) by itself — the
        // result stays over cap after evicting everything evictable. That's
        // the documented soft-cap behavior, not a bug.
        val result = CacheEvictor.evictionsFor(
            listOf(playing, b),
            capBytes = 100,
            currentlyPlaying = TrackId("a"),
        )

        assertEquals(listOf(b), result)
    }

    @Test
    fun `cap of zero evicts everything except the pinned entry`() {
        val playing = entry("a", 10, 1)
        val b = entry("b", 10, 2)
        val c = entry("c", 10, 3)

        val result = CacheEvictor.evictionsFor(listOf(playing, b, c), capBytes = 0, currentlyPlaying = TrackId("a"))

        assertEquals(setOf(b, c), result.toSet())
    }

    @Test
    fun `no currently playing track means every entry is a legitimate eviction candidate`() {
        val a = entry("a", 100, 1)
        val b = entry("b", 100, 2)

        val result = CacheEvictor.evictionsFor(listOf(a, b), capBytes = 0, currentlyPlaying = null)

        assertEquals(setOf(a, b), result.toSet())
    }

    @Test
    fun `is referentially transparent - repeated calls with identical input agree`() {
        // Documents the "no hidden state" guarantee described in
        // CacheEvictor's KDoc: this is what makes it safe to call from
        // multiple threads without any synchronization of its own.
        val entries = listOf(entry("a", 100, 1), entry("b", 100, 2), entry("c", 100, 3))

        val first = CacheEvictor.evictionsFor(entries, capBytes = 120, currentlyPlaying = TrackId("b"))
        val second = CacheEvictor.evictionsFor(entries, capBytes = 120, currentlyPlaying = TrackId("b"))

        assertEquals(first, second)
    }
}
