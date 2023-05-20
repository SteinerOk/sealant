@file:Suppress("UnstableApiUsage")

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "build-logic"

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
    versionCatalogs {
        create("configuration") {
            from(files("../gradle/configuration.toml"))
        }
        create("libs") {
            from(files("../gradle/dependencies.toml"))
        }
    }
}

include(":convention")
