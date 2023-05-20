plugins {
    id("com.android.library")
    id("internal.steinerok.sealant.android-base")
}

@Suppress("UnstableApiUsage")
android {
    defaultConfig {
        consumerProguardFiles("consumer-rules.pro")
    }

    buildFeatures {
        buildConfig = false
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    kotlinOptions {
        // Enable experimental APIs
        freeCompilerArgs = freeCompilerArgs + listOf(
            "-Xexplicit-api=strict",
        )
    }
}
