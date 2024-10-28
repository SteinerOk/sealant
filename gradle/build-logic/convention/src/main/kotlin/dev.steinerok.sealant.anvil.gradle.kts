import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

plugins {
    id("dev.zacsweers.anvil")
}

tasks.withType<KotlinCompilationTask<*>>().configureEach {
    compilerOptions {
        // Enable experimental APIs
        freeCompilerArgs.addAll(
            "-opt-in=com.squareup.anvil.annotations.ExperimentalAnvilApi",
        )
    }
}

anvil {
    syncGeneratedSources.set(true)
    trackSourceFiles.set(true)
}
