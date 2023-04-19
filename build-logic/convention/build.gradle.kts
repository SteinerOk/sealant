plugins {
    // Support convention plugins written in Kotlin. Convention plugins are build scripts in 'src/main' that automatically become available as plugins in the main build.
    `kotlin-dsl`
    // Support convention plugins written in Groovy. Convention plugins are build scripts in 'src/main' that automatically become available as plugins in the main build.
    `groovy-gradle-plugin`
}

group = "dev.helpdesk.buildlogic"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(11))
    }
}

dependencies {
    implementation(libs.android.gradlePlugin)
    implementation(libs.kotlin.gradlePlugin)
    implementation(libs.anvil.gradlePlugin)
    implementation(libs.spotless.gradlePlugin)
    implementation(libs.mavenPublish.default.gradlePlugin)
}
