@file:Suppress("UnstableApiUsage")

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "build-logic-settings"

dependencyResolutionManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
    versionCatalogs {}
}

include(":scan-plugin")
