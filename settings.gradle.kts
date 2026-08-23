pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "music-app"

include(":app")
include(":core:model")
include(":core:data")
include(":core:media")
include(":feature:library")
include(":feature:player")
include(":sync:youtube")
include(":stream")
