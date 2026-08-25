package com.abhishekrathod.musicapp.media

import com.abhishekrathod.musicapp.data.queue.RepeatMode
import com.abhishekrathod.musicapp.model.Track
import kotlinx.coroutines.flow.StateFlow

/**
 * The one door between the UI and playback. Per ARCHITECTURE.md constraint
 * 2 and the Phase C brief: **no screen is allowed to touch a
 * `MediaController`, `MediaSession`, or `PlaybackService` directly** — every
 * screen goes through this interface instead. The payoff is exactly what
 * `:stream`'s `StreamResolver` boundary buys `:core:media` (see
 * ARCHITECTURE.md constraint 1): if Phase B's service/session wiring turns
 * out to be wrong, the fix is in [MediaControllerPlaybackController], never
 * in a screen — and a screen can be unit-tested against a fake
 * implementation of this interface with no real `MediaController` involved
 * at all.
 *
 * Deliberately not a 1:1 wrapper of every `Player`/`MediaController` method —
 * it exposes exactly what the three Phase C screens need (play/pause,
 * seek, next/previous, reorder, repeat, shuffle) and nothing more. A
 * narrower interface is a smaller surface for a screen to misuse.
 */
interface PlaybackController {
    /** The current snapshot. Screens collect this; they never poll a player. */
    val state: StateFlow<PlaybackUiState>

    /** Connects to [PlaybackService]. Safe to call multiple times; a no-op if already connected. */
    fun connect()

    /** Releases the underlying `MediaController`. Call from the owning ViewModel's `onCleared`. */
    fun disconnect()

    /**
     * Resolves every track's stream (via `:stream`'s [com.abhishekrathod.musicapp.stream.StreamResolver] —
     * today, always [com.abhishekrathod.musicapp.stream.FakeStreamResolver], per
     * ARCHITECTURE.md constraint 1) and starts playing [tracks] from [startIndex].
     * Suspends because resolution is the one genuinely asynchronous, fallible
     * step here — everything else below is a direct, synchronous command to
     * an already-connected controller.
     */
    suspend fun setQueueAndPlay(tracks: List<Track>, startIndex: Int)

    fun togglePlayPause()

    fun seekTo(positionMs: Long)

    fun skipToNext()

    fun skipToPrevious()

    /** Jumps directly to the queue item at [index] — e.g. tapping a row in the queue screen. */
    fun skipToQueueItem(index: Int)

    fun moveQueueItem(fromIndex: Int, toIndex: Int)

    fun removeQueueItem(index: Int)

    fun setRepeatMode(mode: RepeatMode)

    fun toggleShuffle()
}
