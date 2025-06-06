plugins {
    `kotlin-dsl`
}

group = "dev.steinerok.buildlogicsettings"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

dependencies {
    implementation("com.gradle.develocity:com.gradle.develocity.gradle.plugin:4.0.2")
}
