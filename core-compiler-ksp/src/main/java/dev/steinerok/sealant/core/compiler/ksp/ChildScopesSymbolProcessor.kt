/*
 * Copyright 2024 Ihor Kushnirenko
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dev.steinerok.sealant.core.compiler.ksp

import com.google.auto.service.AutoService
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.validate
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.ksp.addOriginatingKSFile
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.writeTo
import dev.steinerok.sealant.compiler.ClassNames
import dev.steinerok.sealant.compiler.ClassSpec
import dev.steinerok.sealant.compiler.ConstructorSpec
import dev.steinerok.sealant.compiler.SealantFeature
import dev.steinerok.sealant.compiler.buildVmScopeClassName
import dev.steinerok.sealant.compiler.ksp.SealantFileSpec
import dev.steinerok.sealant.compiler.ksp.getSymbolsWithAnnotation
import dev.steinerok.sealant.compiler.ksp.hasSealantFeatureForScope
import dev.steinerok.sealant.compiler.ksp.requireContainingFile

/**
 * Description of the ViewModel scope marker generation.
 * This generator creates an abstract class that acts as a namespace or
 * structural marker for ViewModel injection within a specific Dagger/Anvil scope.
 */
public class ChildScopesSymbolProcessor(
    private val codeGenerator: CodeGenerator,
    @Suppress("unused") private val options: Map<String, String>,
    @Suppress("unused") private val logger: KSPLogger,
) : SymbolProcessor {

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val (validSymbols, invalidSymbols) = resolver
            .getSymbolsWithAnnotation(ClassNames.sealantConfiguration)
            .filterIsInstance<KSClassDeclaration>()
            .partition { symbol -> symbol.validate() }

        validSymbols
            .forEach { symbol ->
                generateByProcessor(symbol)?.writeTo(
                    codeGenerator = codeGenerator,
                    aggregating = false,
                )
            }

        return invalidSymbols
    }

    private fun generateByProcessor(clazz: KSClassDeclaration): FileSpec? {
        val hasViewModelSupport = clazz.hasSealantFeatureForScope(SealantFeature.ViewModel)
        if (!hasViewModelSupport) return null

        val origClassName = clazz.toClassName()

        val fileName = "${origClassName.simpleName}_SealantChildScopes"
        val fileNode = clazz.requireContainingFile()

        return SealantFileSpec(origClassName.packageName, fileName) {
            // Генерируем ViewModel Scope Marker Class
            addType(buildVmScopeMarkerClass(origClassName, fileNode))
        }
    }

    /**
     * Generates the ViewModel Scope Marker Class.
     * * An abstract class with a private constructor, ensuring it cannot be instantiated.
     * It serves as a foundational type or namespace in the generated code to logically
     * group, attach, or identify ViewModel multibindings and factories associated
     * with the target `<Scope>`.
     * * Output example:
     * ```kotlin
     * public abstract class ViewModel_<Scope> private constructor()
     * ```
     */
    private fun buildVmScopeMarkerClass(
        origClassName: ClassName,
        fileNode: KSFile,
    ): TypeSpec {
        val vmScopeClassName = buildVmScopeClassName(origClassName)

        return ClassSpec(vmScopeClassName) {
            addModifiers(KModifier.ABSTRACT)
            primaryConstructor(
                ConstructorSpec {
                    addModifiers(KModifier.PRIVATE)
                }
            )

            addOriginatingKSFile(fileNode)
        }
    }

    /**
     * Entry point for KSP to pick up our [SymbolProcessor].
     */
    @Suppress("unused")
    @AutoService(SymbolProcessorProvider::class)
    public class Provider : SymbolProcessorProvider {
        override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
            return ChildScopesSymbolProcessor(
                codeGenerator = environment.codeGenerator,
                options = environment.options,
                logger = environment.logger,
            )
        }
    }
}
