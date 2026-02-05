import dev.steinerok.sealant.configVersions
import dev.steinerok.sealant.kotlinCommonCompilerOptionsDefaults

plugins {
    id("org.jetbrains.kotlin.jvm")
}

kotlin {
    //
    explicitApi()
    //
    jvmToolchain(configVersions.javaToolchain.get().toInt())
    //
    compilerOptions {
        kotlinCommonCompilerOptionsDefaults()
    }
}

dependencies {
    // testImplementation(libs.junit4)
}
