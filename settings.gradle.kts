pluginManagement {
    includeBuild("gradle/build-logic-settings")
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

includeBuild("gradle/build-logic")

include(":di-common")
include(":compiler-utils", ":compiler-utils-ksp", ":compiler-utils-embedded")
include(":core-runtime", ":core-compiler-ksp", ":core-compiler-embedded")
include(":appcomponent-runtime", ":appcomponent-compiler-ksp", ":appcomponent-compiler-embedded")
include(":fragment-runtime", ":fragment-compiler-ksp", ":fragment-compiler-embedded")
include(":viewmodel-runtime", ":viewmodel-compiler-ksp", ":viewmodel-compiler-embedded")
include(":work-runtime", ":work-compiler-ksp", ":work-compiler-embedded")
