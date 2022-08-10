package dev.steinerok.sealant.core.generator

import com.squareup.anvil.compiler.api.AnvilContext
import com.squareup.anvil.compiler.api.CodeGenerator

/**
 *
 */
public interface AlwaysApplicableCodeGenerator : CodeGenerator {

    // Behavior is not dependent on factory generation
    override fun isApplicable(context: AnvilContext): Boolean = true
}
