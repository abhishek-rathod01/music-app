// Root build file: declares plugin versions once (via the version catalog) so
// every module applies them without re-specifying a version. `apply false`
// means "make this plugin available, don't apply it to the root project
// itself" — each module opts in individually in its own build.gradle.kts.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.compose.compiler) apply false
}
