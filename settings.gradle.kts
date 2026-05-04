pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "My Application"
include(":app")
include(":feature:camera")
include(":feature:videoplayer")
include(":feature:download")
include(":core:mlkit")
include(":core:network")
include(":core:download")

includeBuild("build-logic")
include(":core:database")
include(":core:navigation")
include(":core:ui")
include(":core:mediaeffect")
include(":core:filament")
include(":feature:renderer")
