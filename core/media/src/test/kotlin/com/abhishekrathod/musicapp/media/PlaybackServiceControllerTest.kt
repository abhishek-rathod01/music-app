package com.abhishekrathod.musicapp.media

import android.content.ComponentName
import android.content.Context
import android.os.Looper
import androidx.media3.common.MediaItem
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
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
 * First attempt used Media3's own `RobolectricUtil.runMainLooperUntil` test
 * helper (from `media3-test-utils-robolectric`) and hit a
 * `NullPointerException` *inside that helper itself* — not at the
 * `controller.isConnected` assertion after it — on the first real CI run.
 * That points at a version-compatibility gap between that helper (built for
 * a specific Robolectric baseline) and this project's Robolectric 4.16.1
 * (see the sourcing note in gradle/libs.versions.toml — a real, flagged risk,
 * not a surprise) rather than a problem with the underlying connection
 * itself. [awaitDone] below replaces it with the plain, stable
 * `ShadowLooper.idle()` primitive directly — a narrower, more
 * verifiable surface — with an explicit timeout so a real failure to
 * connect shows up as a clear assertion message, not an opaque NPE.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class PlaybackServiceControllerTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    /** Pumps the main looper until [future] completes or [timeoutMs] elapses. */
    private fun awaitDone(future: ListenableFuture<*>, timeoutMs: Long = 5_000) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (!future.isDone) {
            assertTrue("Timed out waiting for $future to complete", System.currentTimeMillis() < deadline)
            shadowOf(Looper.getMainLooper()).idle()
        }
    }

    @Test
    fun `a MediaController connects to PlaybackService and can set the sample track`() {
        val sessionToken = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture.addListener({}, MoreExecutors.directExecutor())

        awaitDone(controllerFuture)
        val controller = controllerFuture.get()

        assertTrue(controller.isConnected)

        controller.setMediaItem(MediaItem.fromUri(SampleTrack.uri(context.packageName)))
        controller.prepare()
        shadowOf(Looper.getMainLooper()).idle()

        assertEquals(1, controller.mediaItemCount)

        controller.release()
    }

    @Test
    fun `releasing a MediaController disconnects it cleanly`() {
        val sessionToken = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture.addListener({}, MoreExecutors.directExecutor())
        awaitDone(controllerFuture)
        val controller = controllerFuture.get()
        assertTrue(controller.isConnected)

        controller.release()
        shadowOf(Looper.getMainLooper()).idle()

        assertTrue(!controller.isConnected)
    }
}
