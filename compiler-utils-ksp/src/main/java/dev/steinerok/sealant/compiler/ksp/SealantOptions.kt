package dev.steinerok.sealant.compiler.ksp

import com.google.devtools.ksp.processing.KSPLogger

/** Parsed KSP options that affect how Sealant emits generated code. */
@Suppress("DATA_CLASS_COPY_VISIBILITY_WILL_BE_CHANGED_WARNING")
public data class SealantOptions internal constructor(
    /** Placeholder for future experimental codegen options. */
    val useExperimental: Boolean = false,
) {

    public companion object {

        /** Loads Sealant-specific options from the processor environment. */
        @Suppress("unused")
        public fun load(options: Map<String, String>, logger: KSPLogger): SealantOptions {
            // TODO: read Sealant-specific KSP arguments here as they are introduced.
            return SealantOptions(useExperimental = false)
        }
    }
}
