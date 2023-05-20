import org.gradle.accessors.dm.LibrariesForConfiguration

plugins {
    id("com.android.application")
    id("internal.steinerok.sealant.android-base")
}

// https://github.com/gradle/gradle/issues/15383#issuecomment-779893192
val Project.versions: LibrariesForConfiguration.VersionAccessors
    get() = the<LibrariesForConfiguration>().versions

@Suppress("UnstableApiUsage")
android {
    defaultConfig {
        targetSdk = versions.android.targetSdk.get().toInt()
    }

    buildFeatures {
        buildConfig = true
    }

    packaging {
        resources {
            resources.excludes += listOf(
                "/META-INF/{AL2.0,LGPL2.1}"
            )
        }
    }
}
