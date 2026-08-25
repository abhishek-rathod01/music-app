package com.abhishekrathod.musicapp.media

/**
 * The one bundled local audio file Phase B (and Stage 2) use to prove the
 * playback pipeline end to end before `:stream`'s real resolver exists — see
 * `PLAN.md` Stage 2 and `ARCHITECTURE.md` constraint 1's `FakeStreamResolver`.
 *
 * Exposed as an `android.resource://` content-URI string rather than a
 * generated `R.raw.*` constant, deliberately: `android.nonTransitiveRClass`
 * is on for this project (see root `gradle.properties`), so this module's
 * `R` class isn't visible from `:app`. The symbolic form of the resource URI
 * (`android.resource://<package>/raw/<name>`) resolves against the *final
 * merged* app resources at runtime regardless of which module the file
 * physically lives in, so it works across that module boundary without
 * needing a generated R reference at all.
 */
object SampleTrack {
    private const val RESOURCE_NAME = "sample_track"

    fun uri(applicationId: String): String = "android.resource://$applicationId/raw/$RESOURCE_NAME"
}
