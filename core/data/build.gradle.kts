// :core:data — for tonight's build this holds only the pure-logic half of
// what ARCHITECTURE.md describes for this module: the queue state machine
// and cache eviction policy, neither of which touches Room, DataStore, or
// any other Android API. So, same reasoning as :core:model and :stream, it's
// built as kotlin("jvm") rather than com.android.library right now — fast,
// emulator-free unit tests for logic that has to be exactly right (queue
// bugs are the kind of thing that only shows up as "my music skipped weird"
// on a real device, days later).
//
// This will need to become an Android library module (com.android.library)
// once Room and DataStore land in a future stage — that's a planned,
// expected migration, not a surprise. Flagged here and in PLAN.md so it
// isn't forgotten.
plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(project(":core:model"))

    testImplementation(libs.junit)
}
