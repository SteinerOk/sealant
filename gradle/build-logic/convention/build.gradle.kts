plugins {
    `kotlin-dsl`
}

group = "dev.steinerok.sealant.buildlogic"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

gradlePlugin {
    plugins {
        register("dev.steinerok.sealant.android-application") {
            implementationClass = "AndroidApplicationConventionPlugin"
        }
        register("dev.steinerok.sealant.android-library") {
            implementationClass = "AndroidLibraryConventionPlugin"
        }
    }
}

dependencies {
    // https://github.com/gradle/gradle/issues/15383#issuecomment-779893192
    implementation(files(configuration.javaClass.superclass.protectionDomain.codeSource.location))
    implementation(files(libs.javaClass.superclass.protectionDomain.codeSource.location))

    implementation(libs.kotlin.gradlePlugin)
    implementation(libs.android.gradleApiPlugin)
    implementation(libs.spotless.gradlePlugin)
    implementation(libs.mavenPublish.default.gradlePlugin)
}
