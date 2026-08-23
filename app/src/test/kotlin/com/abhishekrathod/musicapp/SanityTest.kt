package com.abhishekrathod.musicapp

import org.junit.Assert.assertEquals
import org.junit.Test

// Deliberately trivial. Its only job at Stage 1 is to prove that
// testDebugUnitTest actually finds and runs a test in CI — real coverage
// starts once there is real logic to test.
class SanityTest {
    @Test
    fun `unit test task runs`() {
        assertEquals(4, 2 + 2)
    }
}
