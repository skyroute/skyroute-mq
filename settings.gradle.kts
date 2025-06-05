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

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "SkyRouteMQ"

includeBuild("build-logic")

include(":skyroute-mq")
include(":sky-api")
include(":sky-core")
include(":sky-service")

include(":payload-adapter-gson")
include(":payload-adapter-moshi")
include(":payload-adapter-xml")

include(":example-app")
