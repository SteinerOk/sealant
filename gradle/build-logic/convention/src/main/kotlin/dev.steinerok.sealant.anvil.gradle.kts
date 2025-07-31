import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

plugins {
    id("dev.zacsweers.anvil")
}

tasks.withType<KotlinCompilationTask<*>>().configureEach {
    compilerOptions {
        // Enable experimental APIs
        freeCompilerArgs.addAll(
            //
        )
        // Opt-in to experimental APIs
        optIn.addAll(
            "com.squareup.anvil.annotations.ExperimentalAnvilApi",
        )
    }
}

anvil {
    syncGeneratedSources.set(true)
    trackSourceFiles.set(true)
}
