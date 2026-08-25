package com.abhishekrathod.musicapp.stream

import com.abhishekrathod.musicapp.model.TrackId
import java.time.Instant

/**
 * A resolvable audio location. This is deliberately **not** `android.net.Uri`
 * — ARCHITECTURE.md's sketch of this contract uses `Uri`, but `Uri` is an
 * Android framework type, and Phase A of the overnight build that wrote this
 * groups `:stream` under "no Android deps" specifically so it builds as a
 * plain Kotlin/JVM module (see build.gradle.kts) with fast, emulator-free
 * unit tests — exactly the property ARCHITECTURE.md praises `:core:model`
 * for. A `String` URI is trivially convertible to `android.net.Uri` at the
 * one call site in `:core:media` that actually hands it to ExoPlayer
 * (`Uri.parse(resolvedStream.uri)`), so nothing about the contract is lost —
 * this is a deliberate, documented deviation, not an oversight.
 */
@JvmInline
value class StreamUri(val value: String) {
    init {
        require(value.isNotBlank()) { "StreamUri must not be blank" }
    }
}

/**
 * The result of successfully resolving a [TrackId] to something playable.
 *
 * [expiresAt] exists because real resolved URLs are typically short-lived —
 * see ARCHITECTURE.md constraint 1. A `null` here means "doesn't expire, or
 * expiry is unknown"; callers that care about re-resolving before playback
 * should treat `null` conservatively (i.e. as "could be stale"), not as "safe
 * forever". That policy lives in `:core:media`, not here — this module only
 * describes the data, it doesn't decide what to do with it.
 */
data class ResolvedStream(
    val uri: StreamUri,
    val expiresAt: Instant?,
    val mimeType: String?,
)

/**
 * Turns a [TrackId] into something playable. **This is the only interface
 * `:core:media` is allowed to depend on for stream resolution** — see
 * ARCHITECTURE.md constraint 1. Nothing above this interface knows or cares
 * how a [ResolvedStream] was obtained.
 *
 * The `Result` return type is deliberate: resolution failure (a track that
 * can no longer be resolved, a network error, whatever) is an **expected
 * outcome** of calling this, not something exceptional. Wrapping it in
 * `Result` forces every caller to handle the failure case explicitly instead
 * of needing a surrounding `try`/`catch` that's easy to forget — and per
 * CLAUDE.md, a forgotten `catch` around this would be exactly the kind of
 * silent failure that rule exists to prevent.
 */
interface StreamResolver {
    suspend fun resolve(trackId: TrackId): Result<ResolvedStream>
}
