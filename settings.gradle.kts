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

rootProject.name = "steplauncher"

include(":core-ipc")
include(":core-vfs")
include(":core-telemetry")
include(":core-renderer")
include(":app-launcher")
include(":app-widget")
