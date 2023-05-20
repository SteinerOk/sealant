import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.CommonExtension
import com.android.build.api.dsl.LibraryExtension
import org.gradle.accessors.dm.LibrariesForConfiguration

plugins {
    id("org.jetbrains.kotlin.android")
    id("internal.steinerok.sealant.kotlin-compilation")
}

// https://github.com/gradle/gradle/issues/15383#issuecomment-779893192
val Project.versions: LibrariesForConfiguration.VersionAccessors
    get() = the<LibrariesForConfiguration>().versions

@Suppress("UnstableApiUsage")
val commonAndroidConfiguration: CommonExtension<*, *, *, *>.() -> Unit = {
    buildToolsVersion = versions.android.buildToolsVersion.get()
    ndkVersion = versions.android.ndkVersion.get()
    compileSdk = versions.android.compileSdk.get().toInt()

    defaultConfig {
        minSdk = versions.android.minSdk.get().toInt()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // Can remove this once https://issuetracker.google.com/issues/260059413 is fixed.
    // See https://kotlinlang.org/docs/gradle-configure-project.html#gradle-java-toolchains-support
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

inline fun <reified T : CommonExtension<*, *, *, *>> Project.applyBaseAndroidConfiguration() {
    extensions.configure<T> { commonAndroidConfiguration() }
}

pluginManager.withPlugin("com.android.application") {
    applyBaseAndroidConfiguration<ApplicationExtension>()
}

pluginManager.withPlugin("com.android.library") {
    applyBaseAndroidConfiguration<LibraryExtension>()
}

plugins.withType<JavaBasePlugin>().configureEach {
    extensions.configure<JavaPluginExtension> {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(versions.javaToolchain.get().toInt()))
        }
    }
}
