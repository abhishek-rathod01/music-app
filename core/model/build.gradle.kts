// :core:model — pure Kotlin/JVM module, no Android dependencies. Phase A of the
// overnight build groups this with :stream and the logic half of :core:data
// specifically so their tests run as plain JVM tests: no Android SDK, no
// Robolectric, no emulator — just `./gradlew test`. That matters a lot here,
// since the sandbox this was written in can't run an Android build at all
// (see ENVIRONMENT.md) and CI is the only place any of this compiles.
//
// Using kotlin("jvm") instead of com.android.library is what makes that true
// at the build-file level rather than by convention: if someone later adds an
// android.* import here, the module simply won't compile, the same mechanical
// enforcement ARCHITECTURE.md calls out for this module.
plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    testImplementation(libs.junit)
}
