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
import com.google.devtools.ksp.validate
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.ksp.addOriginatingKSFile
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.writeTo
import dev.steinerok.sealant.compiler.ClassNames
import dev.steinerok.sealant.compiler.AnnotationSpec
import dev.steinerok.sealant.compiler.ClassSpec
import dev.steinerok.sealant.compiler.CompanionObjectSpec
import dev.steinerok.sealant.compiler.FunSpec
import dev.steinerok.sealant.compiler.InterfaceSpec
import dev.steinerok.sealant.compiler.ParameterSpec
import dev.steinerok.sealant.compiler.SealantFeature
import dev.steinerok.sealant.compiler.addContributesToAnnotation
import dev.steinerok.sealant.compiler.generateSimpleNameString
import dev.steinerok.sealant.compiler.ksp.SealantFileSpec
import dev.steinerok.sealant.compiler.ksp.findScopesForSealantFeatureIntegration
import dev.steinerok.sealant.compiler.ksp.getSymbolsWithAnnotation
import dev.steinerok.sealant.compiler.ksp.parentScopeWithSealantFeature
import dev.steinerok.sealant.compiler.ksp.requireContainingFile
import dev.steinerok.sealant.compiler.ksp.simpleValidatePredicate

/**
 * Generates the shared fragment-factory infrastructure for every Sealant-enabled scope.
 *
 * The resulting bindings aggregate fragment providers and expose a ready-to-use
 * [dev.steinerok.sealant.fragment.SealantFragmentFactory] from the owning component.
 */
public class FragmentIntegrationSymbolProcessor(
    private val codeGenerator: CodeGenerator,
    @Suppress("unused") private val options: Map<String, String>,
    @Suppress("unused") private val logger: KSPLogger,
) : SymbolProcessor {

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val (validSymbols, invalidSymbols) = resolver
            .getSymbolsWithAnnotation(ClassNames.sealantIntegration)
            .filterIsInstance<KSClassDeclaration>()
            .partition { symbol -> symbol.validate(simpleValidatePredicate) }

        validSymbols
            .flatMap { annotated ->
                annotated
                    .findScopesForSealantFeatureIntegration(SealantFeature.Fragment)
                    .map { annotated to it }
            }
            .distinctBy { it.second }
            .forEach { (clazz, scope) ->
                generateByProcessor(clazz, scope).writeTo(
                    codeGenerator = codeGenerator,
                    aggregating = false,
                )
            }

        return invalidSymbols
    }

    private fun generateByProcessor(
        clazz: KSClassDeclaration,
        scope: KSClassDeclaration,
    ): FileSpec {
        val scopeClassName = scope.toClassName()
        val scopeClassNameStr = scopeClassName.generateSimpleNameString()

        val fileName = "${scopeClassNameStr}_${featureName}_Integration"
        val fileNode = clazz.requireContainingFile()

        return SealantFileSpec(integrationPkg, fileName) {
            // Генерируем Integrative Module только если нет родительского scope с такой фичей
            if (scope.parentScopeWithSealantFeature(SealantFeature.Fragment) == null) {
                addType(buildIntegrativeModule(scopeClassName, scopeClassNameStr, fileNode))
            }

            // Генерируем Fragment Factory Owner Interface
            addType(buildFragmentFactoryOwner(scopeClassName, scopeClassNameStr, fileNode))
        }
    }

    /**
     * Generates the Fragment Integrative Module.
     * * Contributes a module to the `<Scope>` that serves two primary purposes:
     * First, it declares a `@Multibinds` map for Fragments, ensuring the Metro
     * graph compiles successfully even if no specific Fragments have been bound yet.
     * Second, it provides a custom `SealantFragmentFactory` using the aggregated
     * map of Fragment providers. This factory is responsible for instantiating
     * Fragments with their required dependencies injected into their constructors.
     * * Output example:
     * ```kotlin
     * @BindingContainer
     * @ContributesTo(scope = <Scope>::class)
     * public abstract class <Scope>_sealantFragment_IntegrativeModule {
     *
     *     @Multibinds(allowEmpty = true)
     *     public abstract fun bindFragmentMap(): Map<KClass<out Fragment>, Fragment>
     *
     *     public companion object {
     *         @Provides
     *         public fun provideFragmentFactory(
     *             fragmentProviderMap: Map<KClass<out Fragment>, () -> Fragment>
     *         ): SealantFragmentFactory = SealantFragmentFactory(fragmentProviderMap)
     *     }
     * }
     * ```
     */
    private fun buildIntegrativeModule(
        scopeClassName: ClassName,
        scopeClassNameStr: String,
        fileNode: KSFile,
    ): TypeSpec {
        val imNameStr = "${scopeClassNameStr}_${featureName}_IntegrativeModule"
        val imClassName = ClassName(integrationPkg, imNameStr)

        return ClassSpec(imClassName) {
            addModifiers(KModifier.ABSTRACT)
            addAnnotation(ClassNames.bindingContainer)
            addContributesToAnnotation(scopeClassName)

            addFunction(FunSpec("bindFragmentMap") {
                addAnnotation(AnnotationSpec(ClassNames.multibinds) {
                    addMember("allowEmpty = true")
                })
                addModifiers(KModifier.ABSTRACT)
                returns(ClassNames.fragmentMap)
            })

            val companion = CompanionObjectSpec {
                addFunction(FunSpec("provideFragmentFactory") {
                    addAnnotation(ClassNames.provides)
                    addParameter(
                        ParameterSpec("fragmentProviderMap", ClassNames.fragmentProviderMap)
                    )
                    returns(ClassNames.sealantFragmentFactory)
                    addStatement(
                        "return %T(fragmentProviderMap)",
                        ClassNames.sealantFragmentFactory
                    )
                })
            }
            addType(companion)

            addOriginatingKSFile(fileNode)
        }
    }

    /**
     * Generates the Fragment Factory Owner Interface.
     * * Contributes the `SealantFragmentFactoryOwner` interface to the target `<Scope>`.
     * This ensures that the component owning this scope (e.g., an Activity or
     * Application component) officially exposes the `SealantFragmentFactory`.
     * This allows the Android framework (via the `FragmentManager`) to retrieve and
     * use the custom factory at runtime for Fragment creation.
     * * Output example:
     * ```kotlin
     * @ContributesTo(scope = <Scope>::class)
     * public interface <Scope>_SealantFragmentFactoryOwner : SealantFragmentFactory.Owner
     * ```
     */
    private fun buildFragmentFactoryOwner(
        scopeClassName: ClassName,
        scopeClassNameStr: String,
        fileNode: KSFile,
    ): TypeSpec {
        val ffoSnStr = ClassNames.sealantFragmentFactoryOwner.generateSimpleNameString()
        val ffoClassName = ClassName(integrationPkg, "${scopeClassNameStr}_$ffoSnStr")

        return InterfaceSpec(ffoClassName) {
            addSuperinterface(ClassNames.sealantFragmentFactoryOwner)
            addContributesToAnnotation(scopeClassName)

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
            return FragmentIntegrationSymbolProcessor(
                codeGenerator = environment.codeGenerator,
                options = environment.options,
                logger = environment.logger,
            )
        }
    }
}
