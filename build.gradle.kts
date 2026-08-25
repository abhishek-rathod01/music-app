// Root build file: declares plugin versions once (via the version catalog) so
// every module applies them without re-specifying a version. `apply false`
// means "make this plugin available, don't apply it to the root project
// itself" — each module opts in individually in its own build.gradle.kts.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.compose.compiler) apply false
    // Added after CI run #5 failed resolving org.jetbrains.kotlin.jvm inside
    // :stream with "already on the classpath with an unknown version". Root
    // cause (from the actual Gradle error, not guessed): AGP 9's built-in
    // Kotlin embeds Kotlin Gradle Plugin classes as part of resolving
    // com.android.application itself; when a *separate*, explicitly
    // versioned org.jetbrains.kotlin.jvm request happened later, in a
    // different project, Gradle found overlapping classes already present
    // with no version it could verify, and refused. Resolving all three
    // plugins together in one pass, here, at the root — the same
    // `apply false` convention already used for the other two — means
    // Gradle picks a mutually consistent set up front instead of :stream
    // triggering a second, later, colliding resolution.
    alias(libs.plugins.kotlin.jvm) apply false
}
