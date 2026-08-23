// See gradle.properties for why this uses the traditional
// org.jetbrains.kotlin.android plugin rather than AGP 9's new built-in
// Kotlin support.
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "com.abhishekrathod.musicapp"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.abhishekrathod.musicapp"
        minSdk = 26
        targetSdk = 36

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

    kotlinOptions {
        // Must match compileOptions above — Kotlin and Java bytecode
        // targets are independent settings and Gradle won't catch a
        // mismatch between them for you.
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.material3)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.activity.compose)
    debugImplementation(libs.compose.ui.tooling)

    testImplementation(libs.junit)
}
