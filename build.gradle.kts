import java.util.Properties

buildscript {
    dependencies {
        classpath(libs.kotlin.gradlePlugin)
        classpath(libs.ksp.gradlePlugin)
    }
}

plugins {
    id("dev.steinerok.sealant.publish-root")
    alias(libs.plugins.versions)
    alias(libs.plugins.spotless)
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.anvilKsp) apply false
    alias(libs.plugins.metro) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.builtInKotlin) apply false
    alias(libs.plugins.mavenPublish) apply false
}

tasks.clean {
    delete(rootProject.layout.buildDirectory)
}

if (rootProject.file("local.properties").exists()) {
    val localProperties = Properties()
    localProperties.load(rootProject.file("local.properties").inputStream())
    localProperties.forEach { (key, value) -> rootProject.ext.set(key as String, value) }
}
