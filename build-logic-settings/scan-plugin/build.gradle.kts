plugins {
    `kotlin-dsl`
}

group = "dev.steinerok.buildlogicsettings"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(11))
    }
}

dependencies {
    implementation("com.gradle.enterprise:com.gradle.enterprise.gradle.plugin:3.13.2")
}
