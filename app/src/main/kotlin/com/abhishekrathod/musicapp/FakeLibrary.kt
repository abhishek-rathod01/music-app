package com.abhishekrathod.musicapp

import com.abhishekrathod.musicapp.model.Track
import com.abhishekrathod.musicapp.model.TrackId

/**
 * Seed data for the library screen. Real library metadata comes from Room,
 * synced from YouTube — neither exists yet (see PLAN.md Stages 3-4). Adding
 * Room tonight for a handful of hardcoded rows would be real, unplanned
 * scope: schema, DAOs, a migration story, and converting :core:data from a
 * plain Kotlin module back to an Android library module (see its
 * build.gradle.kts) — none of which Phase C's brief asked for. This is a
 * deliberate, flagged scope line, not a shortcut taken quietly.
 *
 * Every track here resolves through the same [com.abhishekrathod.musicapp.stream.FakeStreamResolver]
 * to the one bundled sample WAV (see SampleTrack in :core:media) — exactly
 * what ARCHITECTURE.md's fake-resolver design exists for: proving out
 * multi-track queue behavior with zero network and zero real audio content.
 */
object FakeLibrary {
    val tracks: List<Track> =
        listOf(
            Track(TrackId("fake-1"), "Midnight Drive", "The Night Owls", 214_000, null),
            Track(TrackId("fake-2"), "Paper Boats", "Coral & Bone", 187_000, null),
            Track(TrackId("fake-3"), "Static Bloom", "Vernal", 231_000, null),
            Track(TrackId("fake-4"), "Low Tide", "The Night Owls", 198_000, null),
            Track(TrackId("fake-5"), "Glass Orchard", "Fionna Vale", 245_000, null),
            Track(TrackId("fake-6"), "Six Rooms", "Coral & Bone", 176_000, null),
        )
}
