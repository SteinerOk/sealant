import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

plugins {
    id("dev.steinerok.sealant.kotlin-library")
}

tasks.withType<KotlinCompilationTask<*>>().configureEach {
    compilerOptions {
        // Enable experimental APIs
        freeCompilerArgs.addAll(
            //
        )
        // Opt-in to experimental APIs
        optIn.addAll(
            //
        )
    }
}
