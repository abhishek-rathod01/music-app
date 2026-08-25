package com.abhishekrathod.musicapp.media

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.annotation.Config

/**
 * Exercises the service's own lifecycle in isolation, without a
 * MediaController — see [PlaybackServiceControllerTest] for the
 * session-controller wiring. `Robolectric.buildService` drives real
 * `onCreate`/`onDestroy` calls on a real (JVM-shadowed) `PlaybackService`
 * instance, so these are genuine lifecycle assertions, not mocks.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class PlaybackServiceLifecycleTest {
    @Test
    fun `onCreate builds a session`() {
        val controller = Robolectric.buildService(PlaybackService::class.java).create()

        assertNotNull(controller.get().currentSessionForTests)

        controller.destroy()
    }

    @Test
    fun `onDestroy releases the session`() {
        val controller = Robolectric.buildService(PlaybackService::class.java).create()
        val service = controller.get()

        controller.destroy()

        assertNull(service.currentSessionForTests)
    }

    @Test
    fun `service survives being backgrounded - the session outlives a start command`() {
        // "Backgrounded" for a running foreground service does not mean
        // onDestroy — the process and its Service keep running, which is
        // exactly what requirement M1 depends on. Robolectric's
        // startCommand simulates Android re-delivering onStartCommand
        // (e.g. from a media button or the system) without tearing the
        // service down, unlike destroy().
        val controller = Robolectric.buildService(PlaybackService::class.java).create()
        val service = controller.get()

        controller.startCommand(0, 1)

        assertNotNull(service.currentSessionForTests)

        controller.destroy()
    }
}
