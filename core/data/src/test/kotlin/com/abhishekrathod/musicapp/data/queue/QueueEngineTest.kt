package com.abhishekrathod.musicapp.data.queue

import com.abhishekrathod.musicapp.model.QueueItem
import com.abhishekrathod.musicapp.model.QueueItemId
import com.abhishekrathod.musicapp.model.TrackId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

private fun item(id: String) = QueueItem(QueueItemId(id), TrackId("track-$id"))

private fun queueOf(vararg ids: String, current: String? = null, repeat: RepeatMode = RepeatMode.OFF): Queue {
    val items = ids.map { item(it) }
    return Queue(
        items = items,
        originalOrder = items,
        currentItemId = current?.let { QueueItemId(it) },
        shuffled = false,
        repeatMode = repeat,
    )
}

class QueueAddRemoveTest {
    @Test
    fun `add to empty queue does not auto-select a current item`() {
        val result = QueueEngine.add(Queue.empty(), listOf(item("a"), item("b")))

        assertEquals(2, result.items.size)
        assertNull(result.currentItemId)
    }

    @Test
    fun `add appends after existing items`() {
        val queue = queueOf("a", "b", current = "a")

        val result = QueueEngine.add(queue, listOf(item("c")))

        assertEquals(listOf("a", "b", "c"), result.items.map { it.id.value })
        assertEquals(QueueItemId("a"), result.currentItemId)
    }

    @Test
    fun `add with an empty list is a no-op`() {
        val queue = queueOf("a", "b", current = "a")

        assertEquals(queue, QueueEngine.add(queue, emptyList()))
    }

    @Test
    fun `add keeps originalOrder in sync even while shuffled`() {
        val queue = queueOf("a", "b", current = "a")
        val shuffled = QueueEngine.shuffle(queue, Random(42))

        val result = QueueEngine.add(shuffled, listOf(item("c")))

        assertEquals(listOf("a", "b", "c"), result.originalOrder.map { it.id.value })
    }

    @Test
    fun `remove of an unknown id is a no-op`() {
        val queue = queueOf("a", "b", current = "a")

        assertEquals(queue, QueueEngine.remove(queue, QueueItemId("nope")))
    }

    @Test
    fun `remove of a non-current item leaves current item untouched`() {
        val queue = queueOf("a", "b", "c", current = "a")

        val result = QueueEngine.remove(queue, QueueItemId("b"))

        assertEquals(listOf("a", "c"), result.items.map { it.id.value })
        assertEquals(QueueItemId("a"), result.currentItemId)
    }

    @Test
    fun `remove of the current item advances to the item after it`() {
        val queue = queueOf("a", "b", "c", current = "b")

        val result = QueueEngine.remove(queue, QueueItemId("b"))

        assertEquals(QueueItemId("c"), result.currentItemId)
    }

    @Test
    fun `remove of the current last item with repeat off ends the queue`() {
        val queue = queueOf("a", "b", "c", current = "c", repeat = RepeatMode.OFF)

        val result = QueueEngine.remove(queue, QueueItemId("c"))

        assertNull(result.currentItemId)
    }

    @Test
    fun `remove of the current last item with repeat all wraps to the new first item`() {
        val queue = queueOf("a", "b", "c", current = "c", repeat = RepeatMode.ALL)

        val result = QueueEngine.remove(queue, QueueItemId("c"))

        assertEquals(QueueItemId("a"), result.currentItemId)
    }

    @Test
    fun `remove of the only item empties the queue regardless of repeat mode`() {
        val queue = queueOf("a", current = "a", repeat = RepeatMode.ALL)

        val result = QueueEngine.remove(queue, QueueItemId("a"))

        assertTrue(result.isEmpty)
        assertNull(result.currentItemId)
    }
}

class QueueEmptyAndSingleItemTest {
    @Test
    fun `every operation on an empty queue is a no-op`() {
        val empty = Queue.empty()

        assertEquals(empty, QueueEngine.next(empty))
        assertEquals(empty, QueueEngine.previous(empty))
        assertEquals(empty, QueueEngine.remove(empty, QueueItemId("x")))
        assertEquals(empty, QueueEngine.reorder(empty, 0, 1))
        assertEquals(empty, QueueEngine.setCurrent(empty, QueueItemId("x")))
        assertEquals(empty, QueueEngine.shuffle(empty))
    }

    @Test
    fun `next on a single-item queue with repeat off exhausts the queue`() {
        val queue = queueOf("a", current = "a", repeat = RepeatMode.OFF)

        assertNull(QueueEngine.next(queue).currentItemId)
    }

    @Test
    fun `next on a single-item queue with repeat all wraps to itself`() {
        val queue = queueOf("a", current = "a", repeat = RepeatMode.ALL)

        assertEquals(QueueItemId("a"), QueueEngine.next(queue).currentItemId)
    }

    @Test
    fun `previous on a single-item queue with repeat off clamps at the start`() {
        val queue = queueOf("a", current = "a", repeat = RepeatMode.OFF)

        assertEquals(queue, QueueEngine.previous(queue))
    }
}

class QueueNavigationTest {
    @Test
    fun `next steps forward through the middle of the queue`() {
        val queue = queueOf("a", "b", "c", current = "a")

        assertEquals(QueueItemId("b"), QueueEngine.next(queue).currentItemId)
    }

    @Test
    fun `next with repeat off stops after the last item`() {
        val queue = queueOf("a", "b", "c", current = "c", repeat = RepeatMode.OFF)

        assertNull(QueueEngine.next(queue).currentItemId)
    }

    @Test
    fun `next with repeat all wraps past the last item to the first`() {
        val queue = queueOf("a", "b", "c", current = "c", repeat = RepeatMode.ALL)

        assertEquals(QueueItemId("a"), QueueEngine.next(queue).currentItemId)
    }

    @Test
    fun `repeat-one leaves the queue unchanged on next, including at the last item`() {
        val atStart = queueOf("a", "b", "c", current = "a", repeat = RepeatMode.ONE)
        val atEnd = queueOf("a", "b", "c", current = "c", repeat = RepeatMode.ONE)

        // The whole point of repeat-one: position doesn't matter, "next" is
        // always a no-op on the queue itself (the player restarts the same
        // track — that happens in :core:media, not here).
        assertEquals(atStart, QueueEngine.next(atStart))
        assertEquals(atEnd, QueueEngine.next(atEnd))
    }

    @Test
    fun `repeat-one leaves the queue unchanged on previous too`() {
        val queue = queueOf("a", "b", "c", current = "b", repeat = RepeatMode.ONE)

        assertEquals(queue, QueueEngine.previous(queue))
    }

    @Test
    fun `previous steps backward through the middle of the queue`() {
        val queue = queueOf("a", "b", "c", current = "c")

        assertEquals(QueueItemId("b"), QueueEngine.previous(queue).currentItemId)
    }

    @Test
    fun `previous with repeat all wraps before the first item to the last`() {
        val queue = queueOf("a", "b", "c", current = "a", repeat = RepeatMode.ALL)

        assertEquals(QueueItemId("c"), QueueEngine.previous(queue).currentItemId)
    }

    @Test
    fun `next with nothing selected starts at the first item`() {
        val queue = queueOf("a", "b", "c", current = null)

        assertEquals(QueueItemId("a"), QueueEngine.next(queue).currentItemId)
    }

    @Test
    fun `next with a stale current item id falls back to the first item`() {
        // Simulates an inconsistent state (e.g. a caller-constructed Queue
        // whose currentItemId doesn't exist in items) rather than one this
        // engine could produce on its own — remove() always resolves a
        // valid successor. Still must not crash or get stuck.
        val queue = queueOf("a", "b", "c", current = "nonexistent")

        assertEquals(QueueItemId("a"), QueueEngine.next(queue).currentItemId)
    }

    @Test
    fun `setCurrent jumps directly to the given item`() {
        val queue = queueOf("a", "b", "c", current = "a")

        assertEquals(QueueItemId("c"), QueueEngine.setCurrent(queue, QueueItemId("c")).currentItemId)
    }

    @Test
    fun `setCurrent with an unknown id is a no-op`() {
        val queue = queueOf("a", "b", current = "a")

        assertEquals(queue, QueueEngine.setCurrent(queue, QueueItemId("nope")))
    }

    @Test
    fun `setRepeatMode round-trips through all three modes`() {
        val queue = queueOf("a", current = "a", repeat = RepeatMode.OFF)

        val toOne = QueueEngine.setRepeatMode(queue, RepeatMode.ONE)
        val toAll = QueueEngine.setRepeatMode(toOne, RepeatMode.ALL)
        val toOff = QueueEngine.setRepeatMode(toAll, RepeatMode.OFF)

        assertEquals(RepeatMode.ONE, toOne.repeatMode)
        assertEquals(RepeatMode.ALL, toAll.repeatMode)
        assertEquals(queue, toOff)
    }
}

class QueueShuffleTest {
    @Test
    fun `shuffle preserves the currently playing item`() {
        val queue = queueOf("a", "b", "c", "d", current = "c")

        val shuffled = QueueEngine.shuffle(queue, Random(1))

        assertEquals(QueueItemId("c"), shuffled.currentItemId)
        assertTrue(shuffled.shuffled)
        assertEquals(queue.items.map { it.id }.toSet(), shuffled.items.map { it.id }.toSet())
    }

    @Test
    fun `shuffle then unshuffle restores the original order exactly`() {
        val queue = queueOf("a", "b", "c", "d", current = "b")

        val roundTripped = QueueEngine.unshuffle(QueueEngine.shuffle(queue, Random(7)))

        assertEquals(queue.items, roundTripped.items)
        assertEquals(QueueItemId("b"), roundTripped.currentItemId)
        assertTrue(!roundTripped.shuffled)
    }

    @Test
    fun `reshuffling an already-shuffled queue does not lose the current track`() {
        val queue = queueOf("a", "b", "c", "d", "e", current = "e")

        val once = QueueEngine.shuffle(queue, Random(1))
        val twice = QueueEngine.shuffle(once, Random(2))

        assertEquals(QueueItemId("e"), once.currentItemId)
        assertEquals(QueueItemId("e"), twice.currentItemId)
        assertEquals(queue.items.map { it.id }.toSet(), twice.items.map { it.id }.toSet())
    }

    @Test
    fun `toggleShuffle twice returns to unshuffled original order`() {
        val queue = queueOf("a", "b", "c", current = "a")

        val result = QueueEngine.toggleShuffle(QueueEngine.toggleShuffle(queue, Random(3)), Random(4))

        assertEquals(queue.items, result.items)
        assertTrue(!result.shuffled)
    }
}

class QueueReorderTest {
    @Test
    fun `reorder moves an item and keeps everything else in place`() {
        val queue = queueOf("a", "b", "c", "d", current = "a")

        val result = QueueEngine.reorder(queue, fromIndex = 3, toIndex = 0)

        assertEquals(listOf("d", "a", "b", "c"), result.items.map { it.id.value })
    }

    @Test
    fun `reordering the currently playing item preserves it as current`() {
        val queue = queueOf("a", "b", "c", "d", current = "c")

        val result = QueueEngine.reorder(queue, fromIndex = 2, toIndex = 0)

        assertEquals(listOf("c", "a", "b", "d"), result.items.map { it.id.value })
        assertEquals(QueueItemId("c"), result.currentItemId)
        assertEquals(TrackId("track-c"), result.currentItem?.trackId)
        assertEquals(0, result.currentIndex)
    }

    @Test
    fun `reorder is a no-op when fromIndex equals toIndex`() {
        val queue = queueOf("a", "b", "c", current = "a")

        assertEquals(queue, QueueEngine.reorder(queue, 1, 1))
    }

    @Test
    fun `reorder is a no-op for out-of-range indices`() {
        val queue = queueOf("a", "b", "c", current = "a")

        assertEquals(queue, QueueEngine.reorder(queue, -1, 1))
        assertEquals(queue, QueueEngine.reorder(queue, 0, 99))
    }

    @Test
    fun `reorder does not touch originalOrder`() {
        val queue = queueOf("a", "b", "c", current = "a")

        val result = QueueEngine.reorder(queue, 0, 2)

        assertEquals(queue.originalOrder, result.originalOrder)
    }
}
