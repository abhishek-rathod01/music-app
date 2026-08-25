package com.abhishekrathod.musicapp.model

/**
 * Identifies a track. Backed by a plain string rather than a database row ID
 * because it has to mean the same thing across three independent systems
 * that never talk to each other directly: the synced library (Room row),
 * the queue (which repeats a track without duplicating its metadata), and
 * stream resolution (which only ever needs this to look up a URL). Per
 * ARCHITECTURE.md, this is the one type `:sync:youtube` and `:stream` are
 * both allowed to depend on.
 *
 * Wrapped in a value class instead of a bare `String` so a `TrackId` can
 * never be passed where a display title or artist name was expected (or
 * vice versa) — the compiler catches it, a code reviewer doesn't have to.
 */
@JvmInline
value class TrackId(val value: String) {
    init {
        require(value.isNotBlank()) { "TrackId must not be blank" }
    }
}

/**
 * Track metadata. Immutable — every queue operation in :core:data produces a
 * new [Track]/list rather than mutating one in place, which is what makes
 * that module's state machine safe to reason about and to unit test without
 * worrying about aliasing.
 *
 * `artworkUrl` and `durationMs` are nullable/zero-able because metadata can
 * arrive incomplete (a sync in progress, a track missing thumbnail data) —
 * see CLAUDE.md's "fail loudly" rule: a missing duration should render as an
 * absent value the UI can show plainly, never a silently wrong number like 0
 * masquerading as real data. We use `null` rather than 0 for "unknown" to
 * keep that distinction visible in the type.
 */
data class Track(
    val id: TrackId,
    val title: String,
    val artist: String,
    val durationMs: Long?,
    val artworkUrl: String?,
) {
    init {
        require(title.isNotBlank()) { "Track title must not be blank" }
        require(durationMs == null || durationMs >= 0) { "durationMs must not be negative" }
    }
}
