package com.abhishekrathod.musicapp.model

/**
 * Identifies one *occurrence* of a track in a queue — not the track itself.
 * This distinction is the reason the queue engine in :core:data can be
 * simple: the same [TrackId] can appear in a queue more than once (repeat
 * modes, a track added twice on purpose), and "reorder the currently playing
 * track" or "remove this specific occurrence" needs to name a slot, not a
 * track. Looking up state by [QueueItemId] instead of list index is also
 * what makes reordering-while-playing safe: the currently-playing item is
 * found by id after every mutation, never assumed to still be at the index
 * it used to be at.
 */
@JvmInline
value class QueueItemId(val value: String) {
    init {
        require(value.isNotBlank()) { "QueueItemId must not be blank" }
    }
}

/**
 * One slot in a queue: an occurrence of [trackId] with its own identity.
 * Callers (not the queue engine) are responsible for generating unique
 * [QueueItemId]s when building new items — the engine only ever moves,
 * removes, or looks up items it's given. Keeping id generation outside the
 * engine keeps it a pure function of its inputs: no hidden counter, no
 * randomness, nothing that would make two calls with identical arguments
 * produce different results.
 */
data class QueueItem(
    val id: QueueItemId,
    val trackId: TrackId,
)
