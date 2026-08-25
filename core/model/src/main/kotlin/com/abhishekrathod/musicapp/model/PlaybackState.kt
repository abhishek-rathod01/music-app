package com.abhishekrathod.musicapp.model

/**
 * What the player is doing right now, independent of *what* is queued (see
 * `Queue` in :core:data) — this is playback status, not queue contents.
 * Kept as a plain enum + data holder rather than a sealed class hierarchy
 * per status: every status shares the same two extra fields (position,
 * optional error message) and a sealed hierarchy would mean every consumer
 * pattern-matches to extract fields that are almost always present anyway.
 *
 * `errorMessage` exists because CLAUDE.md requires errors to surface as
 * visible state rather than get swallowed — a [Status.ERROR] with no message
 * would be exactly the kind of silent failure that rule exists to prevent.
 */
enum class Status {
    IDLE,
    BUFFERING,
    PLAYING,
    PAUSED,
    ENDED,
    ERROR,
}

data class PlaybackState(
    val status: Status,
    val positionMs: Long = 0L,
    val errorMessage: String? = null,
) {
    init {
        require(positionMs >= 0) { "positionMs must not be negative" }
        require(status == Status.ERROR || errorMessage == null) {
            "errorMessage must only be set when status is ERROR"
        }
    }

    companion object {
        val Idle = PlaybackState(Status.IDLE)
    }
}
