// AGP 9's default Kotlin support is "built-in": com.android.application
// compiles Kotlin itself, so we don't apply a separate
// org.jetbrains.kotlin.android plugin (see gradle.properties for why —
// the traditional plugin was tried first and fails to compile on this
// AGP version). The Compose compiler is still its own plugin regardless —
// it hooks into the compiler, not into how Kotlin/Android is wired.
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "com.abhishekrathod.musicapp"
    // CI's own dependency check demanded this: the Compose BOM we're on
    // (2026.08.00, Compose 1.12) ships artifacts compiled against API 37,
    // and AGP 9 refuses to let a consumer compile against an older SDK
    // than its dependencies did. Originally set to 36 (Android 16, the
    // stable release at the time) from a web search that couldn't have
    // known that; CI's error named the exact fix.
    compileSdk = 37

    defaultConfig {
        applicationId = "com.abhishekrathod.musicapp"
        minSdk = 26
        targetSdk = 37

        // GITHUB_RUN_NUMBER only exists in CI. Locally (or if anyone ever
        // runs Gradle outside CI) this falls back to a fixed dev value so
        // the build doesn't hard-fail.
        versionCode = System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull() ?: 1
        versionName = "1.0.${System.getenv("GITHUB_RUN_NUMBER") ?: "dev"}"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            // These are only ever set by the release job in
            // .github/workflows/release.yml, from repository secrets. On a
            // pull_request build (or any non-release invocation) every one
            // of these is null, so this signing config is left unconfigured
            // — harmless, because the debug/PR build never applies it.
            val keystorePath = System.getenv("KEYSTORE_PATH")
            if (keystorePath != null) {
                storeFile = file(keystorePath)
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            // No ProGuard/R8 shrinking yet — there's no app code to shrink
            // at this stage, and turning it on later needs real keep rules
            // once Compose/Media3/Room are in the picture.
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    testOptions {
        unitTests {
            // Phase C: Robolectric-backed Compose screen tests need this to
            // resolve string/theme resources inside a plain JVM test — same
            // reasoning as :core:media's identical setting.
            isIncludeAndroidResources = true
        }
    }
}

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.activity.compose)
    debugImplementation(libs.compose.ui.tooling)

    // Phase B: proves the playback pipeline end to end with a minimal
    // screen. :core:media owns the Service; :app owns the MediaController
    // that talks to it, per the brief ("MediaController in the UI layer").
    // media3-session is declared directly here (not inherited from
    // :core:media, which keeps it `implementation`-scoped, i.e. private to
    // that module) because building a MediaController/SessionToken is
    // legitimately UI-layer code, not a :core:media implementation detail.
    implementation(project(":core:media"))
    implementation(libs.media3.session)

    // Phase C CI (run #10) failed compiling :app: FakeLibrary, PlaybackViewModel,
    // and the three screens reference Track/TrackId/RepeatMode/FakeStreamResolver/
    // StreamUri directly, but :core:media keeps its own deps on these modules
    // `implementation`-scoped (private, deliberately -- see its build.gradle.kts),
    // so nothing transitively reached :app. Each needs its own direct dependency.
    implementation(project(":core:model"))
    implementation(project(":core:data"))
    implementation(project(":stream"))

    // Phase C: screens observe PlaybackViewModel via collectAsStateWithLifecycle.
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.runtime.compose)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.test.ext.junit)
    testImplementation(platform(libs.compose.bom))
    testImplementation(libs.compose.ui.test.junit4)
    testImplementation(libs.compose.ui.test.manifest)
}
