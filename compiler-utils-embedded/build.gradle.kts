import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

plugins {
    id("dev.steinerok.sealant.codegen-library")
    id("dev.steinerok.sealant.spotless")
    id("dev.steinerok.sealant.publish-module")
}

kotlin {
    explicitApi()
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompilationTask<*>>().configureEach {
    compilerOptions {
        // Enable experimental APIs
        freeCompilerArgs.addAll(
            "-opt-in=com.squareup.anvil.annotations.ExperimentalAnvilApi",
        )
    }
}

dependencies {
    implementation(libs.kotlinpoet)

    implementation(libs.anvil.compiler.api)
    implementation(libs.anvil.compiler.utils)

    implementation(libs.dagger.runtime)
    implementation(libs.anvil.annotations)

    implementation(projects.sealant.compilerUtils)


    testImplementation(kotlin("test"))
}
