package com.abhishekrathod.musicapp.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class PlaybackStateTest {
    @Test
    fun `rejects negative position`() {
        assertThrows(IllegalArgumentException::class.java) {
            PlaybackState(Status.PLAYING, positionMs = -1L)
        }
    }

    @Test
    fun `rejects an error message on a non-error status`() {
        assertThrows(IllegalArgumentException::class.java) {
            PlaybackState(Status.PLAYING, errorMessage = "boom")
        }
    }

    @Test
    fun `allows an error message when status is ERROR`() {
        val state = PlaybackState(Status.ERROR, errorMessage = "resolution failed")
        assertEquals("resolution failed", state.errorMessage)
    }

    @Test
    fun `Idle constant has no error and zero position`() {
        assertEquals(Status.IDLE, PlaybackState.Idle.status)
        assertEquals(0L, PlaybackState.Idle.positionMs)
        assertEquals(null, PlaybackState.Idle.errorMessage)
    }
}
