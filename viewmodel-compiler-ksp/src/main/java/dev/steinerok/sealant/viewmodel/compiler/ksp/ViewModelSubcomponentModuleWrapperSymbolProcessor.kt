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
package dev.steinerok.sealant.viewmodel.compiler.ksp

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
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.ksp.addOriginatingKSFile
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.writeTo
import dev.steinerok.sealant.compiler.AnnotationSpec
import dev.steinerok.sealant.compiler.ClassNames
import dev.steinerok.sealant.compiler.InterfaceSpec
import dev.steinerok.sealant.compiler.SealantFeature
import dev.steinerok.sealant.compiler.addContributesToAnnotation
import dev.steinerok.sealant.compiler.buildVmScopeClassName
import dev.steinerok.sealant.compiler.ksp.SealantFileSpec
import dev.steinerok.sealant.compiler.ksp.getSymbolsWithAnnotation
import dev.steinerok.sealant.compiler.ksp.hasSealantFeatureForScope
import dev.steinerok.sealant.compiler.ksp.requireContainingFile
import dev.steinerok.sealant.compiler.ksp.scope
import dev.steinerok.sealant.compiler.ksp.simpleValidatePredicate

/**
 * Generates wrapper modules for `@ContributesToViewModel` declarations.
 *
 * The wrapper re-exposes an existing module inside the generated `ViewModel_<Scope>` graph
 * without requiring changes to the original source type.
 */
public class ViewModelSubcomponentModuleWrapperSymbolProcessor(
    private val codeGenerator: CodeGenerator,
    @Suppress("unused") private val options: Map<String, String>,
    @Suppress("unused") private val logger: KSPLogger,
) : SymbolProcessor {

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val (validSymbols, invalidSymbols) = resolver
            .getSymbolsWithAnnotation(ClassNames.contributesToViewModel)
            .filterIsInstance<KSClassDeclaration>()
            .partition { symbol -> symbol.validate(simpleValidatePredicate) }

        validSymbols
            .filter { annotated ->
                annotated
                    .scope()
                    .hasSealantFeatureForScope(SealantFeature.ViewModel)
            }
            .forEach { symbol ->
                generateByProcessor(symbol).writeTo(
                    codeGenerator = codeGenerator,
                    aggregating = false, // Isolating mode
                )
            }

        return invalidSymbols
    }

    private fun generateByProcessor(clazz: KSClassDeclaration): FileSpec {
        val origClassName = clazz.toClassName()
        val origShortName = origClassName.simpleName

        val scopeClassName = clazz.scope().toClassName()
        val vmScopeClassName = buildVmScopeClassName(scopeClassName)

        val fileName = "${origShortName}_Creation"
        val fileNode = clazz.requireContainingFile()

        return SealantFileSpec(origClassName.packageName, fileName) {
            // Генерируем Module Wrapper Interface
            addType(buildModuleWrapper(origClassName, origShortName, vmScopeClassName, fileNode))
        }
    }

    /**
     * Generates the Module Wrapper Interface.
     * * Acts as a structural bridge by utilizing Dagger's `includes` parameter within
     * the `@Module` annotation. It takes the target `<Module>` and contributes it
     * directly to the `<Scope>_ViewModel` via Anvil's `@ContributesTo`. This pattern
     * is highly useful for seamlessly integrating legacy Dagger modules, third-party
     * modules, or shared modules into the Anvil graph without needing to modify
     * their original source code.
     * * Output example:
     * ```kotlin
     * @Module(includes = [<Module>::class])
     * @ContributesTo(scope = <Scope>_ViewModel::class)
     * public interface <Module>_Wrapper
     * ```
     */
    private fun buildModuleWrapper(
        origClassName: ClassName,
        origShortName: String,
        vmScopeClassName: ClassName,
        fileNode: KSFile,
    ): TypeSpec {
        val wNameStr = "${origShortName}_Wrapper"
        val wClassName = ClassName(origClassName.packageName, wNameStr)

        return InterfaceSpec(wClassName) {
            addContributesToAnnotation(vmScopeClassName)
            addAnnotation(AnnotationSpec(ClassNames.module) {
                addMember("includes = [%T::class]", origClassName)
            })

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
            return ViewModelSubcomponentModuleWrapperSymbolProcessor(
                codeGenerator = environment.codeGenerator,
                options = environment.options,
                logger = environment.logger,
            )
        }
    }
}
