package com.abhishekrathod.musicapp.data.queue

import com.abhishekrathod.musicapp.model.QueueItem
import com.abhishekrathod.musicapp.model.QueueItemId
import kotlin.random.Random

/**
 * All queue transitions, as pure functions: `(Queue, args) -> Queue`. No
 * shared state, no side effects, no I/O — everything a caller needs to know
 * about a transition is in its return value. That's what makes this
 * unit-testable by just comparing inputs and outputs (see
 * `QueueEngineTest`), and it's also what Python devs would recognize as
 * "treat state as immutable and write reducers" — the same shape as a
 * Redux-style reducer, if that's a more familiar reference point.
 *
 * Every function is total: no exceptions for "bad" input (empty queue, an
 * out-of-range index, an unknown item id). Invalid input just returns the
 * queue unchanged. A queue driven by drag gestures and rapid button taps
 * will produce transient calls against stale state — a reorder that raced a
 * remove, for instance — and per CLAUDE.md's "fail loudly" rule, silently
 * ignoring a no-op call is not the same as swallowing a real error: nothing
 * here is "wrong", the call is just a no-op against the current state.
 */
object QueueEngine {
    fun add(queue: Queue, newItems: List<QueueItem>): Queue {
        if (newItems.isEmpty()) return queue
        // Appended to the end of both views — including the *shuffled* view
        // if currently shuffled, so newly-added tracks play after everything
        // already queued rather than jumping in at a random shuffled slot.
        return queue.copy(
            items = queue.items + newItems,
            originalOrder = queue.originalOrder + newItems,
        )
    }

    fun remove(queue: Queue, itemId: QueueItemId): Queue {
        if (queue.items.none { it.id == itemId }) return queue

        val newItems = queue.items.filterNot { it.id == itemId }
        val newOriginal = queue.originalOrder.filterNot { it.id == itemId }

        if (queue.currentItemId != itemId) {
            // Removing something other than the current item never moves
            // playback — currentItemId is untouched and still resolves
            // correctly against newItems.
            return queue.copy(items = newItems, originalOrder = newOriginal)
        }

        // Removing the currently-playing item: figure out what should play
        // next, computed against the *pre-removal* order (so "next" means
        // the item that actually came after this one), then land on it in
        // the post-removal list.
        val oldIndex = queue.items.indexOfFirst { it.id == itemId }
        val nextId: QueueItemId? =
            when {
                queue.items.size == 1 -> null // that was the only item — queue is now empty
                oldIndex < queue.items.lastIndex -> queue.items[oldIndex + 1].id
                queue.repeatMode == RepeatMode.ALL -> newItems.firstOrNull()?.id
                else -> null // OFF or ONE, and it was the last item: queue ends
            }
        return queue.copy(items = newItems, originalOrder = newOriginal, currentItemId = nextId)
    }

    /**
     * Moves the item at [fromIndex] to [toIndex] in the current (possibly
     * shuffled) view only — [Queue.originalOrder] is untouched. That's a
     * deliberate choice: shuffle/unshuffle and manual drag-reorder are two
     * independent ordering mechanisms. If you shuffle, manually drag a track
     * to the top, then unshuffle, the manual move is discarded and you're
     * back to strict insertion order — unshuffle always means "go back to
     * how it was added", not "undo my last drag". Out-of-range indices are a
     * no-op rather than an exception — see this object's top-level KDoc.
     */
    fun reorder(queue: Queue, fromIndex: Int, toIndex: Int): Queue {
        if (fromIndex == toIndex) return queue
        if (fromIndex !in queue.items.indices || toIndex !in queue.items.indices) return queue

        val reordered = queue.items.toMutableList()
        val moved = reordered.removeAt(fromIndex)
        reordered.add(toIndex, moved)
        // currentItemId needs no adjustment: it's an id, not an index (see
        // Queue's KDoc), so it still resolves correctly no matter where the
        // item — including the currently-playing one — ends up.
        return queue.copy(items = reordered)
    }

    /** Jumps directly to [itemId], e.g. the user tapped a specific queue row. */
    fun setCurrent(queue: Queue, itemId: QueueItemId): Queue {
        if (queue.items.none { it.id == itemId }) return queue
        return queue.copy(currentItemId = itemId)
    }

    fun setRepeatMode(queue: Queue, mode: RepeatMode): Queue = queue.copy(repeatMode = mode)

    /**
     * Advances playback per [Queue.repeatMode]. If nothing is currently
     * selected (a fresh queue, or a stale [Queue.currentItemId] pointing at
     * a since-removed item), lands on the first item rather than doing
     * nothing — "press next" is a reasonable way to start playback.
     *
     * [RepeatMode.ONE] always returns the queue unchanged, *including at the
     * last item* — repeat-one means "keep restarting this track", so "next"
     * from any position (not just the end) is a no-op on the queue itself;
     * the actual restart is the player seeking to 0, which happens in
     * `:core:media`, not here.
     */
    fun next(queue: Queue): Queue {
        if (queue.isEmpty) return queue

        val currentId = queue.currentItemId ?: return queue.copy(currentItemId = queue.items.first().id)
        val index = queue.items.indexOfFirst { it.id == currentId }
        if (index < 0) return queue.copy(currentItemId = queue.items.first().id) // stale id

        if (queue.repeatMode == RepeatMode.ONE) return queue

        return when {
            index < queue.items.lastIndex -> queue.copy(currentItemId = queue.items[index + 1].id)
            queue.repeatMode == RepeatMode.ALL -> queue.copy(currentItemId = queue.items.first().id)
            else -> queue.copy(currentItemId = null) // OFF, queue exhausted
        }
    }

    /** Mirrors [next] backwards. At the start of the queue with repeat off, clamps (no-op). */
    fun previous(queue: Queue): Queue {
        if (queue.isEmpty) return queue

        val currentId = queue.currentItemId ?: return queue.copy(currentItemId = queue.items.first().id)
        val index = queue.items.indexOfFirst { it.id == currentId }
        if (index < 0) return queue.copy(currentItemId = queue.items.first().id)

        if (queue.repeatMode == RepeatMode.ONE) return queue

        return when {
            index > 0 -> queue.copy(currentItemId = queue.items[index - 1].id)
            queue.repeatMode == RepeatMode.ALL -> queue.copy(currentItemId = queue.items.last().id)
            else -> queue // OFF, already at the start
        }
    }

    /**
     * Randomizes the current view. [Queue.currentItemId] is never touched —
     * only `items`' order changes — so the currently-playing track is never
     * lost by shuffling, including re-shuffling an already-shuffled queue.
     * [random] is injectable so callers (and tests) can get deterministic
     * output; production wiring uses the default.
     */
    fun shuffle(queue: Queue, random: Random = Random.Default): Queue =
        queue.copy(items = queue.items.shuffled(random), shuffled = true)

    /** Restores insertion order. See [reorder]'s KDoc for what this does to a prior manual reorder. */
    fun unshuffle(queue: Queue): Queue = queue.copy(items = queue.originalOrder, shuffled = false)

    fun toggleShuffle(queue: Queue, random: Random = Random.Default): Queue =
        if (queue.shuffled) unshuffle(queue) else shuffle(queue, random)
}
