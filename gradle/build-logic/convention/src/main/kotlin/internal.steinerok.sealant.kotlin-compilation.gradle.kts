import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

tasks.withType<KotlinCompilationTask<*>>().configureEach {
    compilerOptions {
        freeCompilerArgs.addAll(
            "-Xcontext-parameters",
        )
        optIn.addAll(
            "kotlin.RequiresOptIn",
            "kotlin.ExperimentalStdlibApi",
            "kotlin.ExperimentalMultiplatform",
            "kotlin.time.ExperimentalTime",
            "kotlin.contracts.ExperimentalContracts",
        )
    }
}
