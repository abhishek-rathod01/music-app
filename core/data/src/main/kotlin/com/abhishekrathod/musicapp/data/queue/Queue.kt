package com.abhishekrathod.musicapp.data.queue

import com.abhishekrathod.musicapp.model.QueueItem
import com.abhishekrathod.musicapp.model.QueueItemId

/**
 * Immutable queue state. Every [QueueEngine] operation takes one [Queue] and
 * returns a new one — nothing here is ever mutated in place, which is what
 * makes the whole state machine safe to reason about and to test by
 * comparing inputs and outputs directly.
 *
 * The design choice that makes reordering and shuffling safe:
 * **[currentItemId] is an id, not an index.** An index into [items] would
 * have to be manually re-derived on every add/remove/reorder/shuffle, and
 * getting that arithmetic wrong is exactly the kind of bug that only shows
 * up as "the wrong track started playing" on a real phone. Looking the
 * current item up by id instead means every mutation either finds it again
 * (nothing surprising happens) or doesn't (it was removed, handled
 * explicitly) — there's no third, silently-wrong case.
 *
 * [originalOrder] is the insertion-order view, kept in sync with [items] on
 * every add/remove *regardless* of shuffle state. It exists solely so
 * [QueueEngine.unshuffle] has something to restore to. A manual reorder
 * (drag-and-drop) only ever touches [items] — see [QueueEngine.reorder]'s
 * KDoc for what that means if you shuffle, reorder, then unshuffle.
 */
data class Queue(
    val items: List<QueueItem>,
    val originalOrder: List<QueueItem>,
    val currentItemId: QueueItemId?,
    val shuffled: Boolean,
    val repeatMode: RepeatMode,
) {
    val currentIndex: Int?
        get() = currentItemId?.let { id -> items.indexOfFirst { it.id == id }.takeIf { it >= 0 } }

    val currentItem: QueueItem?
        get() = currentItemId?.let { id -> items.firstOrNull { it.id == id } }

    val isEmpty: Boolean
        get() = items.isEmpty()

    companion object {
        fun empty(repeatMode: RepeatMode = RepeatMode.OFF): Queue =
            Queue(
                items = emptyList(),
                originalOrder = emptyList(),
                currentItemId = null,
                shuffled = false,
                repeatMode = repeatMode,
            )
    }
}
