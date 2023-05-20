import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

plugins {
    id("com.squareup.anvil")
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
}
