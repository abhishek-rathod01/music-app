package com.abhishekrathod.musicapp.stream

import com.abhishekrathod.musicapp.model.TrackId

/**
 * Resolves every [TrackId] to the same bundled local file, unconditionally
 * succeeding. This is what lets everything upstream of `:stream` — the
 * queue, the player, the UI — be built and tested (on a phone, once Phase B
 * exists) with zero network dependency and zero YouTube code, per
 * ARCHITECTURE.md constraint 1.
 *
 * [localUri] is injected rather than hardcoded so the same class serves both
 * the real app (pointed at the bundled `res/raw` audio file wired up in
 * `:core:media`, e.g. `"android.resource://<package>/raw/sample_track"`) and
 * tests (pointed at any fixture string, since nothing here actually touches
 * a filesystem or the network).
 *
 * [failFor] is an optional injection point for tests that need to exercise
 * the failure path this interface's `Result` return type exists for — see
 * StreamResolver's KDoc. It defaults to "never fails" so production wiring
 * doesn't have to think about it.
 */
class FakeStreamResolver(
    private val localUri: StreamUri,
    private val mimeType: String? = "audio/mpeg",
    private val failFor: Set<TrackId> = emptySet(),
) : StreamResolver {
    override suspend fun resolve(trackId: TrackId): Result<ResolvedStream> {
        if (trackId in failFor) {
            return Result.failure(
                StreamResolutionException("FakeStreamResolver configured to fail for $trackId"),
            )
        }
        return Result.success(
            ResolvedStream(uri = localUri, expiresAt = null, mimeType = mimeType),
        )
    }
}

/**
 * Marker exception so failures surfaced through [StreamResolver.resolve]'s
 * `Result` are identifiable as resolution failures specifically, rather than
 * an arbitrary [Exception] a caller has to guess the origin of — another
 * "fail loudly" consequence: a caller catching this by type knows exactly
 * what broke.
 */
class StreamResolutionException(message: String) : Exception(message)
