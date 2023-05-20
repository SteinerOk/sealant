import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

tasks.withType<KotlinCompilationTask<*>>().configureEach {
    compilerOptions {
        freeCompilerArgs.addAll(
            "-Xcontext-receivers",
            "-Xjvm-default=all",
            "-opt-in=kotlin.RequiresOptIn",
            "-opt-in=kotlin.ExperimentalStdlibApi",
            "-opt-in=kotlin.ExperimentalMultiplatform",
            "-opt-in=kotlin.time.ExperimentalTime",
            "-opt-in=kotlin.contracts.ExperimentalContracts",
        )
    }
}
