package dev.steinerok.sealant

import org.jetbrains.kotlin.gradle.dsl.KotlinCommonCompilerOptions

/**
 * Applies default compiler options and opt-ins intended for this project.
 */
internal val kotlinCommonCompilerOptionsDefaults: KotlinCommonCompilerOptions.() -> Unit = {
    // Enable experimental functionality
    // Common compiler options are applied to all Kotlin source sets
    freeCompilerArgs.addAll(
        "-Xexplicit-backing-fields",
        "-Xcontext-parameters",
        "-Xexpect-actual-classes",
        "-Xannotations-in-metadata",
        "-Xcontext-sensitive-resolution",
        "-Xannotation-target-all",
        "-Xannotation-default-target=param-property",
        "-Xreturn-value-checker=check",
    )

    // Enable experimental APIs
    optIn.addAll(
        "kotlin.ExperimentalMultiplatform",
        "kotlin.ExperimentalStdlibApi",
        "kotlin.ExperimentalUnsignedTypes",
        "kotlin.RequiresOptIn",
        "kotlin.concurrent.atomics.ExperimentalAtomicApi",
        "kotlin.contracts.ExperimentalContracts",
        "kotlin.experimental.ExperimentalTypeInference",
        "kotlin.io.encoding.ExperimentalEncodingApi",
        "kotlin.io.path.ExperimentalPathApi",
        "kotlin.time.ExperimentalTime",
        "kotlin.uuid.ExperimentalUuidApi",
        "kotlinx.coroutines.ExperimentalCoroutinesApi",
        "kotlinx.coroutines.FlowPreview",
    )

    // Treat all Kotlin warnings as errors (disabled by default)
    // allWarningsAsErrors.set(true)

    // Enable extra compiler checks
    // extraWarnings.set(true)

    // Temporary disable warning in compilation output
    // suppressWarnings.set(true)

    // Enable Progressive Mode (optional, but recommended for strict projects)
    // Makes the compiler treat some warnings as errors if they will become errors in future versions
    // progressiveMode.set(true)
}
