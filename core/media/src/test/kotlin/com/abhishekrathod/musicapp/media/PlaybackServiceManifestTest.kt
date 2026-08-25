package com.abhishekrathod.musicapp.media

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Asserts the three manifest requirements from the overnight brief directly
 * against the merged manifest, via PackageManager — not by re-reading the
 * XML file as text, which would only prove the XML says the right thing,
 * not that Android would actually parse and honor it the way we intend.
 *
 * Pinned to API 34 ([Config.sdk]) rather than this project's compileSdk 37:
 * Robolectric 4.16.1's confirmed support tops out at "Baklava" (SDK 36) per
 * the version research in gradle/libs.versions.toml, and API 34 is exactly
 * where FOREGROUND_SERVICE_MEDIA_PLAYBACK became mandatory — the most
 * meaningful level to test this at regardless.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class PlaybackServiceManifestTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun `service declares the mediaPlayback foreground service type`() {
        val info =
            context.packageManager.getServiceInfo(
                ComponentName(context, PlaybackService::class.java),
                PackageManager.GET_META_DATA,
            )

        assertEquals(ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK, info.foregroundServiceType)
    }

    @Test
    fun `service resolves for the MediaSessionService action`() {
        val intent = Intent("androidx.media3.session.MediaSessionService").setPackage(context.packageName)

        val resolved = context.packageManager.queryIntentServices(intent, 0)

        assertTrue(resolved.any { it.serviceInfo.name == PlaybackService::class.java.name })
    }

    @Test
    fun `app requests POST_NOTIFICATIONS`() {
        val packageInfo =
            context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS)

        assertTrue(
            packageInfo.requestedPermissions.orEmpty().contains("android.permission.POST_NOTIFICATIONS"),
        )
    }

    @Test
    fun `app requests FOREGROUND_SERVICE_MEDIA_PLAYBACK`() {
        val packageInfo =
            context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS)

        assertTrue(
            packageInfo.requestedPermissions.orEmpty()
                .contains("android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK"),
        )
    }
}
