// :stream — the isolation boundary from ARCHITECTURE.md constraint 1. Pure
// Kotlin/JVM, same reasoning as :core:model (see its build.gradle.kts): the
// build file itself is what stops this module from ever importing an
// android.* type or a YouTube-specific one, not just a comment saying not
// to. It must never depend on :sync:youtube — see constraint 3.
plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(project(":core:model"))
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
