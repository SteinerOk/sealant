import org.gradle.accessors.dm.LibrariesForConfiguration
import org.gradle.accessors.dm.LibrariesForConfiguration.VersionAccessors
import org.gradle.accessors.dm.LibrariesForLibs

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("internal.steinerok.sealant.kotlin-compilation")
}

// https://github.com/gradle/gradle/issues/15383#issuecomment-779893192
val Project.versions: VersionAccessors
    get() = the<LibrariesForConfiguration>().versions
val Project.libs: LibrariesForLibs
    get() = the()

kotlin {
    //
    explicitApi()
    //
    jvmToolchain(versions.javaToolchain.get().toInt())
}

dependencies {
    // testImplementation(libs.junit4)
}
