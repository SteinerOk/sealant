import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

plugins {
    id("dev.steinerok.sealant.kotlin-library")
}

tasks.withType<KotlinCompilationTask<*>>().configureEach {
    compilerOptions {
        // Enable experimental APIs
        freeCompilerArgs.addAll(
            "-opt-in=com.squareup.anvil.annotations.ExperimentalAnvilApi",
        )
    }
}
