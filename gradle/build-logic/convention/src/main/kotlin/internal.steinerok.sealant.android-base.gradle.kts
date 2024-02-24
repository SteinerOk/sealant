import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.CommonExtension
import com.android.build.api.dsl.LibraryExtension
import org.gradle.accessors.dm.LibrariesForConfiguration
import org.gradle.accessors.dm.LibrariesForConfiguration.VersionAccessors
import org.gradle.accessors.dm.LibrariesForLibs

plugins {
    id("org.jetbrains.kotlin.android")
    id("internal.steinerok.sealant.kotlin-compilation")
}

// https://github.com/gradle/gradle/issues/15383#issuecomment-779893192
val Project.versions: VersionAccessors
    get() = the<LibrariesForConfiguration>().versions
val Project.libs: LibrariesForLibs
    get() = the()

@Suppress("UnstableApiUsage")
val commonAndroidConfiguration: CommonExtension<*, *, *, *, *>.() -> Unit = {
    buildToolsVersion = versions.android.buildToolsVersion.get()
    ndkVersion = versions.android.ndkVersion.get()
    compileSdk = versions.android.compileSdk.get().toInt()

    defaultConfig {
        minSdk = versions.android.minSdk.get().toInt()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    dependencies {
        testImplementation(libs.junit4)
        androidTestImplementation(libs.junit4)
        androidTestImplementation(libs.androidx.test.core)
        androidTestImplementation(libs.androidx.test.coreKtx)
        androidTestImplementation(libs.androidx.test.ext.junit)
        androidTestImplementation(libs.androidx.test.ext.junitKtx)
    }
}

plugins.withType<JavaBasePlugin>().configureEach {
    extensions.configure<JavaPluginExtension> {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(versions.javaToolchain.get().toInt()))
        }
    }
}

pluginManager.withPlugin("com.android.application") {
    the<ApplicationExtension>().commonAndroidConfiguration()
}

pluginManager.withPlugin("com.android.library") {
    the<LibraryExtension>().commonAndroidConfiguration()
}

fun DependencyHandler.testImplementation(dependencyNotation: Any) =
    add("testImplementation", dependencyNotation)

fun DependencyHandler.androidTestImplementation(dependencyNotation: Any) =
    add("androidTestImplementation", dependencyNotation)
