pluginManagement {
    includeBuild("build-logic-settings")
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
    plugins {}
    resolutionStrategy {}
}

plugins {
    id("convention-scan")
}

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
    versionCatalogs {
        create("configuration") {
            from(files("./gradle/configuration.toml"))
        }
        create("libs") {
            from(files("./gradle/dependencies.toml"))
        }
    }
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "sealant"

includeBuild("build-logic")

include(":di-common")
include(":core-api", ":core-codegen")
include(":appcomponent-api", ":appcomponent-codegen")
include(":fragment-api", ":fragment-codegen")
include(":viewmodel-api", ":viewmodel-codegen")
include(":work-api", ":work-codegen")

include(":sample:core-di", ":sample:feature-entrance", ":sample:feature-home", ":sample:app")
