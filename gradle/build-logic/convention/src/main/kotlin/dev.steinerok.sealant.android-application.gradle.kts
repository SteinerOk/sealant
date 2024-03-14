import com.android.build.api.variant.ApplicationVariant
import org.gradle.accessors.dm.LibrariesForConfiguration
import org.gradle.accessors.dm.LibrariesForConfiguration.VersionAccessors

plugins {
    id("com.android.application")
    id("internal.steinerok.sealant.android-base")
}

// https://github.com/gradle/gradle/issues/15383#issuecomment-779893192
val Project.versions: VersionAccessors
    get() = the<LibrariesForConfiguration>().versions

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
                "**.properties",
                "**/*.kotlin_*",
                "/META-INF/*.md",
                "/META-INF/*.txt",
                "/META-INF/*.dot",
                "/META-INF/{AL2.0,LGPL2.1}",
                "/META-INF/NOTICE",
                "/META-INF/README",
                "/META-INF/LICENSE",
                "/META-INF/CHANGES",
                "/META-INF/DEPENDENCIES",
                "NOTICE",
                "README",
                "LICENSE",
                "CHANGES",
                "DEPENDENCIES",
            )
        }
    }
}

androidComponents {
    val resourcesExcludesCallback: (variant: ApplicationVariant) -> Unit = { variant ->
        variant.packaging.resources.excludes.addAll(
            // Only exclude *.version files in release mode as a debug mode requires these files
            // for layout inspector to work.
            "/META-INF/*.version",
            // DebugProbesKt.bin is not used when debugging Coroutines on Android.
            "DebugProbesKt.bin"
        )
    }
    onVariants(selector().withBuildType("release"), resourcesExcludesCallback)
    onVariants(selector().withBuildType("internal"), resourcesExcludesCallback)
}
