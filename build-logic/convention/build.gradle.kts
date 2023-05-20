plugins {
    `kotlin-dsl`
}

group = "dev.helpdesk.buildlogic"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(11))
    }
}

dependencies {
    // https://github.com/gradle/gradle/issues/15383#issuecomment-779893192
    implementation(files(configuration.javaClass.superclass.protectionDomain.codeSource.location))
    implementation(files(libs.javaClass.superclass.protectionDomain.codeSource.location))

    implementation(libs.android.gradlePlugin)
    implementation(libs.kotlin.gradlePlugin)
    implementation(libs.anvil.gradlePlugin)
    implementation(libs.spotless.gradlePlugin)
    implementation(libs.mavenPublish.default.gradlePlugin)
}
