@file:Suppress("UnstableApiUsage")

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "build-logic"

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
    versionCatalogs {
        create("libs") {
            from(files("../gradle/dependencies-latest.toml"))
        }
        create("stableLibs") {
            from(files("../gradle/dependencies-stable.toml"))
        }
    }
}

include(":convention")
