package dev.steinerok.sealant.compiler.ksp

import com.google.devtools.ksp.processing.KSPLogger

/** Parsed KSP options that affect how Sealant emits generated code. */
@Suppress("DATA_CLASS_COPY_VISIBILITY_WILL_BE_CHANGED_WARNING")
public data class SealantOptions internal constructor(
    /** Enables the Metro interop code path for generated bindings. */
    val useMetroInterop: Boolean,
) {

    public companion object {
        private const val MODE = "sealant.codegen.mode"

        /** Loads Sealant-specific options from the processor environment. */
        @Suppress("unused")
        public fun load(options: Map<String, String>, logger: KSPLogger): SealantOptions {
            val useMetroInterop = when (val mode = options[MODE]) {
                "metroInterop" -> true
                null, "anvilKsp" -> false
                else -> throw IllegalArgumentException("Unrecognised option for codegen mode \"$mode\".")
            }
            return SealantOptions(useMetroInterop = useMetroInterop)
        }
    }
}
