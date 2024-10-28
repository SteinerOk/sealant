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
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ksp.addOriginatingKSFile
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.writeTo
import dev.steinerok.sealant.compiler.ClassNames
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

/**
 * Should generate:
 * ```
 * @Module
 * @ContributesTo(scope = <Scope>::class)
 * public abstract class <Scope>_sealantFragment_IntegrativeModule {
 *
 *     @Multibinds
 *     public abstract fun bindFragmentMap(): Map<Class<out Fragment>, Fragment>
 *
 *     public companion object {
 *         @Provides
 *         public fun provideFragmentFactory(
 *             fragmentProviderMap: Map<Class<out Fragment>, @JvmSuppressWildcards Provider<Fragment>>
 *         ): sealantFragmentFactory = sealantFragmentFactory(fragmentProviderMap)
 *     }
 * }
 *
 * @ContributesTo(scope = <Scope>::class)
 * public interface <Scope>_SealantFragmentFactoryOwner : SealantFragmentFactory.Owner
 * ```
 */
public class FragmentIntegrationSymbolProcessor(
    private val codeGenerator: CodeGenerator,
    @Suppress("unused") private val options: Map<String, String>,
    @Suppress("unused") private val logger: KSPLogger,
) : SymbolProcessor {

    override fun process(resolver: Resolver): List<KSAnnotated> {
        resolver
            .getSymbolsWithAnnotation(ClassNames.sealantIntegration)
            .filterIsInstance<KSClassDeclaration>()
            .map { annotated ->
                annotated
                    .findScopesForSealantFeatureIntegration(SealantFeature.Fragment)
                    .map { annotated to it }
            }
            .flatten()
            .distinctBy { it.second }
            .onEach { _ -> /* Verification if you need */ }
            .forEach { symbol ->
                generateByProcessor(symbol.first, symbol.second).writeTo(
                    codeGenerator = codeGenerator,
                    aggregating = false,
                )
            }
        return emptyList()
    }

    private fun generateByProcessor(
        clazz: KSClassDeclaration,
        scope: KSClassDeclaration
    ): FileSpec {
        val packageName = integrationPkg
        val scopeClassName = scope.toClassName()
        val scopeClassNameStr = scopeClassName.generateSimpleNameString()
        val fileName = "${scopeClassNameStr}_${featureName}_Integration"
        //
        val content = SealantFileSpec(packageName, fileName) {
            if (scope.parentScopeWithSealantFeature(SealantFeature.Fragment) == null) {
                //
                val imNameStr = "${scopeClassNameStr}_${featureName}_IntegrativeModule"
                val imClassName = ClassName(packageName, imNameStr)
                val imClass = ClassSpec(imClassName) {
                    addModifiers(KModifier.ABSTRACT)
                    addAnnotation(ClassNames.module)
                    addContributesToAnnotation(scopeClassName)
                    addFunction(
                        FunSpec("bindFragmentMap") {
                            addAnnotation(ClassNames.multibinds)
                            addModifiers(KModifier.ABSTRACT)
                            returns(ClassNames.fragmentMap)
                        }
                    )
                    val companion = CompanionObjectSpec {
                        addFunction(
                            FunSpec("provideFragmentFactory") {
                                addAnnotation(ClassNames.provides)
                                addParameter(
                                    ParameterSpec(
                                        "fragmentProviderMap",
                                        ClassNames.fragmentProviderMap
                                    )
                                )
                                returns(ClassNames.sealantFragmentFactory)
                                addStatement(
                                    "return·%T(fragmentProviderMap)",
                                    ClassNames.sealantFragmentFactory
                                )
                            }
                        )
                    }
                    addType(companion)
                    addOriginatingKSFile(clazz.requireContainingFile())
                }
                addType(imClass)
            }
            //
            val ffoNameStr =
                "${scopeClassNameStr}_${ClassNames.sealantFragmentFactoryOwner.generateSimpleNameString()}"
            val ffoClassName = ClassName(packageName, ffoNameStr)
            val ffoInterface = InterfaceSpec(ffoClassName) {
                addSuperinterface(ClassNames.sealantFragmentFactoryOwner)
                addContributesToAnnotation(scopeClassName)
                addOriginatingKSFile(clazz.requireContainingFile())
            }
            addType(ffoInterface)
        }
        return content
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
