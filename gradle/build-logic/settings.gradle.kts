enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "build-logic"

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    versionCatalogs {
        create("configuration") {
            from(files("../configuration.toml"))
        }
        create("libs") {
            from(files("../dependencies.toml"))
        }
    }
}

include(":convention")
