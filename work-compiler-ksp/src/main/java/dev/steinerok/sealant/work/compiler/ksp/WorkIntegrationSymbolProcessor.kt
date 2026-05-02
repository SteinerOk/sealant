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
package dev.steinerok.sealant.work.compiler.ksp

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
 * Generates the shared WorkManager integration for Sealant-enabled scopes.
 *
 * The emitted types collect worker assisted factories into a multibinding map and expose the
 * resulting [dev.steinerok.sealant.work.SealantWorkerFactory] from the owning component.
 */
public class WorkIntegrationSymbolProcessor(
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
                    .findScopesForSealantFeatureIntegration(SealantFeature.Work)
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
            if (scope.parentScopeWithSealantFeature(SealantFeature.Work) == null) {
                addType(buildIntegrativeModule(scopeClassName, scopeClassNameStr, fileNode))
            }

            // Генерируем Worker Factory Owner Interface
            addType(buildWorkerFactoryOwner(scopeClassName, scopeClassNameStr, fileNode))
        }
    }

    /**
     * Generates the Worker Integrative Module.
     * * Contributes a module to the `<Scope>` that manages the collection and provisioning
     * of Worker factories. First, it declares a `@Multibinds` map annotated with
     * `@SealantWorkerAssistedFactoryMap` to aggregate all individual Worker factories safely,
     * even if the map is empty. Second, it provides the central `SealantWorkerFactory`,
     * injecting the populated map of providers so it can delegate Worker instantiation
     * to the correct assisted factory at runtime.
     * * Output example:
     * ```kotlin
     * @Module
     * @ContributesTo(scope = <Scope>::class)
     * public abstract class <Scope>_SealantWork_IntegrativeModule {
     *
     *     @Multibinds
     *     @SealantWorkerAssistedFactoryMap
     *     public abstract fun bindWafMap(): Map<String, WorkerAssistedFactory<out ListenableWorker>>
     *
     *     public companion object {
     *         @Provides
     *         public fun provideWorkerFactory(
     *             @SealantWorkerAssistedFactoryMap wafProviderMap: Map<String, @JvmSuppressWildcards Provider<WorkerAssistedFactory<out ListenableWorker>>>
     *         ): SealantWorkerFactory = SealantWorkerFactory(wafProviderMap)
     *     }
     * }
     * ```
     */
    private fun buildIntegrativeModule(
        scopeClassName: ClassName,
        scopeClassNameStr: String,
        fileNode: KSFile
    ): TypeSpec {
        val wimNameStr = "${scopeClassNameStr}_${featureName}_IntegrativeModule"
        val wimClassName = ClassName(integrationPkg, wimNameStr)

        return ClassSpec(wimClassName) {
            addModifiers(KModifier.ABSTRACT)
            addAnnotation(ClassNames.module)
            addContributesToAnnotation(scopeClassName)

            addFunction(FunSpec(name = "bindWafMap") {
                addAnnotation(ClassNames.multibinds)
                addAnnotation(ClassNames.sealantWorkerAssistedFactoryMap)
                addModifiers(KModifier.ABSTRACT)
                returns(ClassNames.workerAssistedFactoryMap)
            })

            val companion = CompanionObjectSpec {
                addFunction(FunSpec(name = "provideWorkerFactory") {
                    addAnnotation(ClassNames.provides)
                    addParameter(
                        ParameterSpec(
                            name = "wafProviderMap",
                            type = ClassNames.workerAssistedFactoryProviderMap
                        ) {
                            addAnnotation(ClassNames.sealantWorkerAssistedFactoryMap)
                        }
                    )
                    returns(ClassNames.sealantWorkerFactory)
                    addStatement(
                        "return %T(wafProviderMap)",
                        ClassNames.sealantWorkerFactory
                    )
                })
            }
            addType(companion)

            addOriginatingKSFile(fileNode)
        }
    }

    /**
     * Generates the Worker Factory Owner Interface.
     * * Contributes the `SealantWorkerFactory_Owner` interface to the target `<Scope>`.
     * This ensures the owning component (typically the Application component) officially
     * exposes the configured `SealantWorkerFactory`. This is crucial because the Android
     * Application class needs to retrieve this factory to initialize the `WorkManager`
     * configuration during app startup.
     * * Output example:
     * ```kotlin
     * @ContributesTo(scope = <Scope>::class)
     * public interface <Scope>_SealantWorkerFactory_Owner : SealantWorkerFactory.Owner
     * ```
     */
    private fun buildWorkerFactoryOwner(
        scopeClassName: ClassName,
        scopeClassNameStr: String,
        fileNode: KSFile,
    ): TypeSpec {
        val wfoSnStr = ClassNames.sealantWorkerFactoryOwner.generateSimpleNameString()
        val wfoClassName = ClassName(integrationPkg, "${scopeClassNameStr}_$wfoSnStr")

        return InterfaceSpec(wfoClassName) {
            addSuperinterface(ClassNames.sealantWorkerFactoryOwner)
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
            return WorkIntegrationSymbolProcessor(
                codeGenerator = environment.codeGenerator,
                options = environment.options,
                logger = environment.logger,
            )
        }
    }
}
