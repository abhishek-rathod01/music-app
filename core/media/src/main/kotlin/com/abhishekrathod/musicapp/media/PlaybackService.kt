package com.abhishekrathod.musicapp.media

import android.content.Intent
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

/**
 * Owns the one and only [ExoPlayer] and [MediaSession] for the app. Per
 * ARCHITECTURE.md constraint 2, this is deliberately the *only* place a
 * player instance is created — a ViewModel or Activity would die with its
 * UI, but a `MediaSessionService` outlives it, which is what lets playback
 * survive screen-off, app-switch, and doze (requirement M1).
 *
 * The implementation follows the exact pattern from the official Android
 * Developers guide (developer.android.com/media/media3/session/background-playback)
 * on purpose — a lot of tutorials for this API are stale and lead you to
 * hand-roll things (like a NotificationManager) that Media3 already does
 * correctly for you. Specifically: **we never touch NotificationManager
 * directly.** `MediaSessionService` auto-generates a `MediaStyle`
 * notification from the session's own state — title, artist, artwork,
 * transport controls — and keeps it in sync as that state changes. Building
 * one by hand would mean re-deriving all of that ourselves and it would fall
 * out of sync the moment we forgot an edge case.
 */
class PlaybackService : MediaSessionService() {
    private var mediaSession: MediaSession? = null

    /**
     * Test-only window into [mediaSession] — `internal` so it's visible to
     * this module's own test source set but not part of the module's public
     * API. Exists so tests can assert lifecycle behavior (a session exists
     * after `onCreate`, is gone after `onDestroy`) without fabricating a
     * `MediaSession.ControllerInfo`, which isn't meant to be constructed
     * outside the framework.
     */
    internal val currentSessionForTests: MediaSession?
        get() = mediaSession

    override fun onCreate() {
        super.onCreate()
        val player =
            ExoPlayer.Builder(this)
                // Required so playback keeps pulling data over the network
                // (or, once :stream's real resolver exists, re-resolving an
                // expiring URL) even once the CPU would otherwise sleep —
                // exactly the scenario M1 has to survive. WAKE_MODE_NETWORK
                // (not WAKE_MODE_LOCAL or _NONE) because streamed audio
                // needs the radio kept alive, not just the CPU.
                .setWakeMode(C.WAKE_MODE_NETWORK)
                .build()
        mediaSession = MediaSession.Builder(this, player).build()
    }

    /**
     * Every `MediaController` connection (the UI, in Phase C; Android Auto,
     * in a later stage) goes through here. Returning the same session for
     * every caller means every controller talks to the same playback state —
     * there is only ever one.
     */
    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    /**
     * Official-pattern behavior for "user swiped the app away from recent
     * tasks": stop playback and let the service (and its foreground
     * notification) go away, rather than continuing to play from an app the
     * user just dismissed. `@OptIn(UnstableApi)` because
     * `pauseAllPlayersAndStopSelf` is itself marked `@UnstableApi` in Media3
     * — a library-stability annotation, not a sign this is unproven; it's
     * the method the official guide itself recommends.
     */
    @OptIn(UnstableApi::class)
    override fun onTaskRemoved(rootIntent: Intent?) {
        pauseAllPlayersAndStopSelf()
    }

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }
}
