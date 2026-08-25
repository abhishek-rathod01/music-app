package com.abhishekrathod.musicapp.media

import android.content.Context
import android.os.Looper
import androidx.media3.common.MediaItem
import androidx.media3.session.MediaController
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * Phase D adversarial pass on [PlaybackService]/`MediaController` wiring,
 * beyond the happy-path connect-and-play case already covered by
 * [PlaybackServiceControllerTest]: rapid connect/disconnect churn, a
 * controller going away mid-playback, and a controller that never gets a
 * queue at all. Uses the same `session.token`-based connection approach as
 * that file, for the same Robolectric-specific reason documented there.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class PlaybackServiceAdversarialTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    private fun awaitDone(future: ListenableFuture<*>, timeoutMs: Long = 5_000) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (!future.isDone) {
            assertTrue("Timed out waiting for $future to complete", System.currentTimeMillis() < deadline)
            shadowOf(Looper.getMainLooper()).idle()
        }
    }

    private fun connectController(session: androidx.media3.session.MediaSession): MediaController {
        val future = MediaController.Builder(context, session.token).buildAsync()
        future.addListener({}, MoreExecutors.directExecutor())
        awaitDone(future)
        return future.get()
    }

    @Test
    fun `repeated connect-release cycles against the same service each leave it in a working state`() {
        val serviceController = Robolectric.buildService(PlaybackService::class.java).create()
        val session = requireNotNull(serviceController.get().currentSessionForTests)

        // Five short-lived controllers in a row -- e.g. a UI that connects
        // on every onStart and releases on every onStop, as ours does.
        // Nothing here should leak state that breaks the next connection.
        repeat(5) { attempt ->
            val controller = connectController(session)
            assertTrue("connection attempt #$attempt should succeed", controller.isConnected)
            controller.release()
            shadowOf(Looper.getMainLooper()).idle()
        }

        // The service itself is still alive and can hand out one more
        // working connection after all that churn.
        val finalController = connectController(session)
        assertTrue(finalController.isConnected)
        finalController.release()
        serviceController.destroy()
    }

    @Test
    fun `a controller releasing while media is loaded and playing does not affect a second controller`() {
        val serviceController = Robolectric.buildService(PlaybackService::class.java).create()
        val session = requireNotNull(serviceController.get().currentSessionForTests)

        val first = connectController(session)
        first.setMediaItem(MediaItem.fromUri(SampleTrack.uri(context.packageName)))
        first.prepare()
        first.play()
        shadowOf(Looper.getMainLooper()).idle()

        // The controller that was actually driving playback disappears --
        // e.g. the Activity is killed -- without pausing first.
        first.release()
        shadowOf(Looper.getMainLooper()).idle()

        // A fresh controller (e.g. the UI reappearing) must still be able
        // to connect to the same session and see the player's real state,
        // not crash or hang because the previous controller vanished.
        val second = connectController(session)
        assertTrue(second.isConnected)
        assertEquals(1, second.mediaItemCount)

        second.release()
        serviceController.destroy()
    }

    @Test
    fun `a controller can connect and prepare against an empty queue without crashing`() {
        val serviceController = Robolectric.buildService(PlaybackService::class.java).create()
        val session = requireNotNull(serviceController.get().currentSessionForTests)

        val controller = connectController(session)
        assertTrue(controller.isConnected)

        // No setMediaItem(s) call at all -- the state a fresh install or a
        // just-cleared queue would be in. prepare()/play() on nothing must
        // be a safe no-op, not a crash.
        controller.prepare()
        controller.play()
        shadowOf(Looper.getMainLooper()).idle()

        assertEquals(0, controller.mediaItemCount)
        assertTrue(!controller.isPlaying)

        controller.release()
        serviceController.destroy()
    }
}
