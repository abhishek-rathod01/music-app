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
 * The riskiest test in this phase, said plainly: connecting a real
 * `MediaController` to a real `MediaSessionService` is an asynchronous,
 * cross-process-shaped operation (Media3 treats it that way even though
 * Robolectric runs everything in one JVM process), and this exact
 * combination — this Robolectric version, this Media3 version, this AGP/
 * Kotlin setup — has never run anywhere before this CI job.
 *
 * Two prior CI attempts on this file, both root-caused from the real stack
 * trace rather than guessed:
 * 1. `RobolectricUtil.runMainLooperUntil` (media3-test-utils-robolectric)
 *    NPE'd internally — a version-compat gap with this project's
 *    Robolectric 4.16.1, not a connection problem. Replaced with a
 *    hand-written `awaitDone()` using only the stable `ShadowLooper.idle()`
 *    primitive.
 * 2. That fix never even ran: the NPE moved one line earlier, to
 *    `SessionToken(context, ComponentName(...))` itself. That constructor
 *    (per its real source) calls `PackageManager.getApplicationInfo(...)`
 *    and `queryIntentServices(...)` synchronously to resolve the target
 *    service's type — a two-step, PackageManager-mediated lookup that
 *    apparently doesn't fully hold up under this Robolectric setup.
 *
 * This version sidesteps that lookup entirely rather than fighting it: a
 * `MediaSession` already knows its own [androidx.media3.session.SessionToken]
 * — `session.token` — obtained in-process from a service instance built
 * directly via `Robolectric.buildService`, with no `ComponentName`/
 * `PackageManager` resolution involved at all. If this still doesn't
 * connect, that's a real finding about this Robolectric+Media3 combination's
 * limits, not something to keep guessing at — see BLOCKERS.md.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class PlaybackServiceControllerTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    private fun awaitDone(future: ListenableFuture<*>, timeoutMs: Long = 5_000) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (!future.isDone) {
            assertTrue("Timed out waiting for $future to complete", System.currentTimeMillis() < deadline)
            shadowOf(Looper.getMainLooper()).idle()
        }
    }

    @Test
    fun `a MediaController connects to PlaybackService and can set the sample track`() {
        val serviceController = Robolectric.buildService(PlaybackService::class.java).create()
        val session = requireNotNull(serviceController.get().currentSessionForTests)

        val controllerFuture = MediaController.Builder(context, session.token).buildAsync()
        controllerFuture.addListener({}, MoreExecutors.directExecutor())
        awaitDone(controllerFuture)
        val controller = controllerFuture.get()

        assertTrue(controller.isConnected)

        controller.setMediaItem(MediaItem.fromUri(SampleTrack.uri(context.packageName)))
        controller.prepare()
        shadowOf(Looper.getMainLooper()).idle()

        assertEquals(1, controller.mediaItemCount)

        controller.release()
        serviceController.destroy()
    }

    @Test
    fun `releasing a MediaController disconnects it cleanly`() {
        val serviceController = Robolectric.buildService(PlaybackService::class.java).create()
        val session = requireNotNull(serviceController.get().currentSessionForTests)

        val controllerFuture = MediaController.Builder(context, session.token).buildAsync()
        controllerFuture.addListener({}, MoreExecutors.directExecutor())
        awaitDone(controllerFuture)
        val controller = controllerFuture.get()
        assertTrue(controller.isConnected)

        controller.release()
        shadowOf(Looper.getMainLooper()).idle()

        assertTrue(!controller.isConnected)
        serviceController.destroy()
    }
}
