package dev.steinerok.sealant

import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

/**
 * Configure an Android module with common settings (Application or Library).
 */
internal fun Project.configureAndroidCommon() {
    androidCommon {
        buildToolsVersion = configVersions.android.buildToolsVersion.get()
        ndkVersion = configVersions.android.ndkVersion.get()

        compileSdk {
            version = release(configVersions.android.compileSdk.get().toInt()) {
                configVersions.android.compileSdkMinor.get().takeIf { it != "-" }
                    ?.let { compileSdkMinor -> minorApiLevel = compileSdkMinor.toInt() }
                configVersions.android.compileSdkExtension.get().takeIf { it != "-" }
                    ?.let { compileSdkExt -> sdkExtension = compileSdkExt.toInt() }
            }
        }

        defaultConfig.apply {
            minSdk {
                version = release(configVersions.android.minSdk.get().toInt())
            }

            testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }

        buildTypes.apply {
            getByName("release") {
                proguardFiles(
                    getDefaultProguardFile("proguard-android-optimize.txt"),
                    "proguard-rules.pro"
                )
            }
        }
    }

    dependencies {
        testImplementation(libs.junit4)

        androidTestImplementation(libs.junit4)
        androidTestImplementation(libs.androidx.test.coreKtx)
        androidTestImplementation(libs.androidx.test.ext.junitKtx)
        androidTestImplementation(libs.androidx.test.espresso.core)
    }

    kotlinCommon {
        jvmToolchain(configVersions.javaToolchain.get().toInt())
    }
    tasks.withType<KotlinCompilationTask<*>>().configureEach {
        compilerOptions {
            kotlinCommonCompilerOptionsDefaults()
        }
    }
}

/**
 * Configure specific settings for an Android Library module.
 */
internal fun Project.configureAndroidLibrary() {
    configureAndroidCommon()

    androidLibrary {
        defaultConfig {
            consumerProguardFiles("consumer-rules.pro")
        }

        buildFeatures {
            buildConfig = false
        }

        buildTypes {
            release {
                isMinifyEnabled = false     // Enable in the future
            }
        }
    }
}

/**
 * Configure specific settings for an Android Application module.
 */
internal fun Project.configureAndroidApplication() {
    configureAndroidCommon()

    androidApplication {
        defaultConfig {
            targetSdk {
                version = release(configVersions.android.targetSdk.get().toInt())
            }
        }

        buildFeatures {
            buildConfig = true
        }
    }
}
