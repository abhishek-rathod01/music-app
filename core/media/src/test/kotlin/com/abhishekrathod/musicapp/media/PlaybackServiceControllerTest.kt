package com.abhishekrathod.musicapp.media

import android.content.ComponentName
import android.content.Context
import android.os.Looper
import androidx.media3.common.MediaItem
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.media3.test.utils.robolectric.RobolectricUtil
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
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
 * Kotlin setup — has never run anywhere before this CI job. If this needs a
 * follow-up push to get the looper-idling right, that's expected, not a
 * sign the underlying wiring is broken; see ENVIRONMENT.md.
 *
 * Uses Media3's own official test helper, [RobolectricUtil.runMainLooperUntil],
 * rather than a hand-rolled sleep/idle loop — the whole point of the Phase B
 * brief's "tutorials are often wrong here" warning is to prefer
 * library-provided testing utilities over reinvented ones.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class PlaybackServiceControllerTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun `a MediaController connects to PlaybackService and can set the sample track`() {
        val sessionToken = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture.addListener({}, MoreExecutors.directExecutor())

        RobolectricUtil.runMainLooperUntil { controllerFuture.isDone }
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
        RobolectricUtil.runMainLooperUntil { controllerFuture.isDone }
        val controller = controllerFuture.get()
        assertTrue(controller.isConnected)

        controller.release()
        shadowOf(Looper.getMainLooper()).idle()

        assertTrue(!controller.isConnected)
    }
}
