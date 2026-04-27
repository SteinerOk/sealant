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
package dev.steinerok.sealant.fragment.compiler.ksp

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
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.validate
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.ksp.addOriginatingKSFile
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.writeTo
import dev.steinerok.sealant.compiler.AnnotationSpec
import dev.steinerok.sealant.compiler.ClassNames
import dev.steinerok.sealant.compiler.FunSpec
import dev.steinerok.sealant.compiler.InterfaceSpec
import dev.steinerok.sealant.compiler.ParameterSpec
import dev.steinerok.sealant.compiler.SealantFeature
import dev.steinerok.sealant.compiler.addContributesToAnnotation
import dev.steinerok.sealant.compiler.ksp.SealantFileSpec
import dev.steinerok.sealant.compiler.ksp.argumentOfTypeAtOrNull
import dev.steinerok.sealant.compiler.ksp.getSymbolsWithAnnotation
import dev.steinerok.sealant.compiler.ksp.hasSealantFeatureForScope
import dev.steinerok.sealant.compiler.ksp.implements
import dev.steinerok.sealant.compiler.ksp.requireAnnotation
import dev.steinerok.sealant.compiler.ksp.requireContainingFile
import dev.steinerok.sealant.compiler.ksp.scope

/**
 * Generates fragment multibinding entries for classes annotated with `@ContributesFragment`.
 *
 * Each generated module adds a constructor-injected fragment to the map later consumed by
 * [dev.steinerok.sealant.fragment.SealantFragmentFactory].
 */
public class FragmentCreationSymbolProcessor(
    private val codeGenerator: CodeGenerator,
    @Suppress("unused") private val options: Map<String, String>,
    @Suppress("unused") private val logger: KSPLogger,
) : SymbolProcessor {

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val (validSymbols, invalidSymbols) = resolver
            .getSymbolsWithAnnotation(ClassNames.contributesFragment)
            .filterIsInstance<KSClassDeclaration>()
            .partition { symbol -> symbol.validate() }

        validSymbols
            .filter { annotated ->
                annotated
                    .scope()
                    .hasSealantFeatureForScope(SealantFeature.Fragment)
            }
            .forEach { symbol ->
                generateByProcessor(symbol)?.writeTo(
                    codeGenerator = codeGenerator,
                    aggregating = false,
                )
            }

        return invalidSymbols
    }

    private fun generateByProcessor(clazz: KSClassDeclaration): FileSpec? {
        if (!clazz.implements(ClassNames.androidxFragment)) {
            logger.error(
                message = "The annotation `@ContributesFragment` can only be applied " +
                        "to classes which extend ${ClassNames.androidxFragment}",
                symbol = clazz
            )
            return null
        }

        val origClassName = clazz.toClassName()
        val scopeClassName = clazz.scope().toClassName()

        val fileName = "${origClassName.simpleName}_Creation"
        val fileNode = clazz.requireContainingFile()

        return SealantFileSpec(origClassName.packageName, fileName) {
            addType(buildFragmentBindsModule(origClassName, scopeClassName, clazz, fileNode))
        }
    }

    /**
     * Generates the Fragment Binds Module.
     * * Contributes a binding module to the target `<Scope>`. By combining `@Binds`,
     * `@IntoMap`, and the custom `@FragmentKey`, it instructs Dagger to map the
     * specific Fragment `<Type>` to its base `Fragment` class within a Multibinding Map.
     * This allows the dependency graph to locate and instantiate the correct Fragment
     * at runtime based on its class type.
     * * Output example:
     * ```kotlin
     * @Module
     * @ContributesTo(scope = <Scope>::class)
     * public interface <Type>_BindsModule {
     *     @Binds
     *     @IntoMap
     *     @FragmentKey(<Type>::class)
     *     public fun bind(instance: <Type>): Fragment
     * }
     * ```
     */
    private fun buildFragmentBindsModule(
        origClassName: ClassName,
        scopeClassName: ClassName,
        clazz: KSClassDeclaration,
        fileNode: KSFile,
    ): TypeSpec {
        val bmNameStr = "${origClassName.simpleName}_BindsModule"
        val bmClassName = ClassName(origClassName.packageName, bmNameStr)

        return InterfaceSpec(bmClassName) {
            addAnnotation(ClassNames.module)
            addContributesToAnnotation(scopeClassName) {
                val replaces = clazz
                    .requireAnnotation(ClassNames.contributesFragment)
                    .argumentOfTypeAtOrNull<List<KSType>>("replaces")
                    ?.mapNotNull { (it.declaration as? KSClassDeclaration)?.toClassName() }
                    .orEmpty()
                if (replaces.isNotEmpty()) {
                    val replacesStr = replaces
                        .joinToString(prefix = "[", postfix = "]") { "%T::class" }
                    addMember("replaces = $replacesStr", *replaces.toTypedArray())
                }
            }

            addFunction(FunSpec("bind") {
                addAnnotation(ClassNames.binds)
                addAnnotation(ClassNames.intoMap)
                addAnnotation(AnnotationSpec(ClassNames.fragmentKey) {
                    addMember("%T::class", origClassName)
                })
                addModifiers(KModifier.ABSTRACT)
                addParameter(ParameterSpec("instance", origClassName))
                returns(ClassNames.androidxFragment)
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
            return FragmentCreationSymbolProcessor(
                codeGenerator = environment.codeGenerator,
                options = environment.options,
                logger = environment.logger,
            )
        }
    }
}
