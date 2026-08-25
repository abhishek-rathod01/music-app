package com.abhishekrathod.musicapp.data.cache

import com.abhishekrathod.musicapp.model.TrackId

/**
 * One cached track's bookkeeping. Keyed by [TrackId] rather than a resolved
 * URL — per ARCHITECTURE.md, resolved URLs are short-lived and re-resolved
 * on each play, so keying the cache on the URL would defeat caching almost
 * entirely (a re-resolved URL would look like a fresh, uncached track every
 * time).
 */
data class CacheEntry(
    val trackId: TrackId,
    val sizeBytes: Long,
    val lastAccessedAtMs: Long,
) {
    init {
        require(sizeBytes >= 0) { "sizeBytes must not be negative" }
    }
}
