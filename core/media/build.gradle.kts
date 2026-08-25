// :core:media — the one module allowed to touch Media3/ExoPlayer/MediaSession,
// per ARCHITECTURE.md constraint 2 (playback lives in a MediaSessionService,
// never a ViewModel or Activity). Unlike Phase A's modules, this genuinely
// needs Android — a Service, a MediaSession, real ExoPlayer — so it's
// com.android.library with AGP's built-in Kotlin (same as :app), not
// kotlin("jvm"). Its tests run under Robolectric: real Android framework
// behavior on the JVM, no emulator, which matches what ENVIRONMENT.md says
// this sandbox and CI can actually do.
plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.abhishekrathod.musicapp.media"
    compileSdk = 37

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    testOptions {
        unitTests {
            // Robolectric needs this to load manifest/resource-backed
            // behavior (e.g. resolving the bundled sample-track resource)
            // inside a plain JVM unit test.
            isIncludeAndroidResources = true
        }
    }
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:data"))
    implementation(project(":stream"))
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.session)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.test.ext.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.media3.test.utils.robolectric)
}
