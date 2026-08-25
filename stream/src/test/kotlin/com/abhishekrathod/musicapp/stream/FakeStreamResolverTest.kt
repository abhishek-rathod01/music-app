package com.abhishekrathod.musicapp.stream

import com.abhishekrathod.musicapp.model.TrackId
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FakeStreamResolverTest {
    private val localUri = StreamUri("android.resource://com.abhishekrathod.musicapp/raw/sample_track")

    @Test
    fun `resolves any track id to the configured local uri`() = runTest {
        val resolver = FakeStreamResolver(localUri)

        val result = resolver.resolve(TrackId("anything"))

        assertTrue(result.isSuccess)
        assertEquals(localUri, result.getOrThrow().uri)
    }

    @Test
    fun `never-expiring result has a null expiresAt`() = runTest {
        val resolver = FakeStreamResolver(localUri)

        val stream = resolver.resolve(TrackId("t1")).getOrThrow()

        assertEquals(null, stream.expiresAt)
    }

    @Test
    fun `two different track ids both resolve successfully to the same uri`() = runTest {
        // The fake is deliberately track-agnostic: it proves out the queue,
        // player, and UI layers without ever needing real per-track content.
        val resolver = FakeStreamResolver(localUri)

        val a = resolver.resolve(TrackId("a")).getOrThrow()
        val b = resolver.resolve(TrackId("b")).getOrThrow()

        assertEquals(a.uri, b.uri)
    }

    @Test
    fun `fails only for track ids in the configured failure set`() = runTest {
        val badTrack = TrackId("bad")
        val resolver = FakeStreamResolver(localUri, failFor = setOf(badTrack))

        val badResult = resolver.resolve(badTrack)
        val goodResult = resolver.resolve(TrackId("good"))

        assertFalse(badResult.isSuccess)
        assertTrue(badResult.exceptionOrNull() is StreamResolutionException)
        assertTrue(goodResult.isSuccess)
    }

    @Test
    fun `custom mime type is passed through`() = runTest {
        val resolver = FakeStreamResolver(localUri, mimeType = "audio/flac")

        val stream = resolver.resolve(TrackId("t1")).getOrThrow()

        assertEquals("audio/flac", stream.mimeType)
    }
}

class StreamUriTest {
    @Test
    fun `rejects blank uri`() {
        org.junit.Assert.assertThrows(IllegalArgumentException::class.java) { StreamUri("") }
    }
}
