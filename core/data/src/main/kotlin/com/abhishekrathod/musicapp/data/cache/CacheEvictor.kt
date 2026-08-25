package com.abhishekrathod.musicapp.data.cache

import com.abhishekrathod.musicapp.model.TrackId

/**
 * Decides what to evict, given the current cache contents — it does not
 * touch a filesystem or a database. The actual cache (`:core:media`, once
 * Media3's `CacheDataSource` is wired up in a later stage) calls this with
 * its current bookkeeping and deletes whatever comes back. Splitting it out
 * this way means the *policy* ("what's LRU, what's pinned, how much to free")
 * is a plain function you can unit test with a list of entries and a number,
 * with no fake filesystem or in-memory database needed to exercise it.
 *
 * Being a pure function also directly answers the "concurrent access" case
 * called out in the overnight brief: there's no shared mutable state in this
 * object for two threads to race on. Calling [evictionsFor] twice with the
 * same arguments, from any number of threads, always returns equal results —
 * see `CacheEvictorTest`'s determinism test. Whatever mutable cache state
 * eventually exists in `:core:media` still needs its own synchronization
 * when this function's *result* is applied — that responsibility is the
 * caller's, not something this pure decision function can or should own.
 */
object CacheEvictor {
    /**
     * LRU by [CacheEntry.lastAccessedAtMs]: oldest-accessed evicted first.
     * [currentlyPlaying], if given, is **never** evicted, even if evicting it
     * would be the only way to get under [capBytes] — playing a track and
     * having it vanish out from under playback would be a worse failure mode
     * than the cache briefly running over its cap. In that situation this
     * evicts everything else evictable and returns, still over cap: a soft
     * target, not a hard guarantee, and the caller should treat it as such
     * rather than assert the result always fits.
     */
    fun evictionsFor(
        entries: List<CacheEntry>,
        capBytes: Long,
        currentlyPlaying: TrackId?,
    ): List<CacheEntry> {
        val totalBytes = entries.sumOf { it.sizeBytes }
        if (totalBytes <= capBytes) return emptyList()

        val evictable = entries.filter { it.trackId != currentlyPlaying }.sortedBy { it.lastAccessedAtMs }

        val toEvict = mutableListOf<CacheEntry>()
        var remaining = totalBytes
        for (entry in evictable) {
            if (remaining <= capBytes) break
            toEvict += entry
            remaining -= entry.sizeBytes
        }
        return toEvict
    }
}
