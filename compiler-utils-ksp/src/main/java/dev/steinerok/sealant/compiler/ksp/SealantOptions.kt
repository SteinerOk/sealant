package dev.steinerok.sealant.compiler.ksp

import com.google.devtools.ksp.processing.KSPLogger

/**
 *
 */
@Suppress("DATA_CLASS_COPY_VISIBILITY_WILL_BE_CHANGED_WARNING")
public data class SealantOptions internal constructor(
    val useMetro: Boolean,
) {

    public companion object {
        private const val MODE = "sealant.codegen.mode"

        @Suppress("unused")
        public fun load(options: Map<String, String>, logger: KSPLogger): SealantOptions {
            val useMetro = when (val mode = options[MODE]) {
                "metro" -> true
                null, "anvilKsp" -> false
                else -> throw IllegalArgumentException("Unrecognised option for codegen mode \"$mode\".")
            }
            return SealantOptions(useMetro = useMetro)
        }
    }
}
