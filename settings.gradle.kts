// See: https://docs.gradle.org/current/userguide/plugins.html#sec:plugin_version_management
// See: https://github.com/gradle/gradle/tree/master/subprojects/docs/src/samples/android-application
@Suppress("UnstableApiUsage")
pluginManagement {
    includeBuild("gradle/build-logic-settings")
    repositories {
        gradlePluginPortal {
            content {
                excludeGroupAndSubgroups("androidx")
                excludeGroupAndSubgroups("com.android")
                excludeGroupAndSubgroups("com.google")
            }
        }
        google()
        mavenCentral()
    }
    plugins {}
    resolutionStrategy {}
}

plugins {
    id("convention-develocity")
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google {
            content {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal {
            content {
                includeGroupAndSubgroups("org.gradle")
            }
        }
    }
    versionCatalogs {
        create("configuration") {
            from(files("./gradle/configuration.versions.toml"))
        }
    }
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "sealant"

includeBuild("gradle/build-logic")

include(":di-common")
include(":compiler-utils", ":compiler-utils-ksp")
include(":core-runtime", ":core-compiler-ksp")
include(":appcomponent-runtime", ":appcomponent-compiler-ksp")
include(":fragment-runtime", ":fragment-compiler-ksp")
include(":viewmodel-runtime", ":viewmodel-compiler-ksp")
include(":work-runtime", ":work-compiler-ksp")
include(":version-catalog")

val sampleDirectory = file("sample")
if (sampleDirectory.exists() && sampleDirectory.isDirectory) {
    include(
        ":sample:anvil:core-di",
        ":sample:anvil:feature-entrance",
        ":sample:anvil:feature-home",
        ":sample:anvil:app"
    )
}
