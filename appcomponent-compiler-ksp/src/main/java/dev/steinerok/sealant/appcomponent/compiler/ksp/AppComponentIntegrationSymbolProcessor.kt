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
package dev.steinerok.sealant.appcomponent.compiler.ksp

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
import dev.steinerok.sealant.compiler.FunSpec
import dev.steinerok.sealant.compiler.InterfaceSpec
import dev.steinerok.sealant.compiler.SealantFeature
import dev.steinerok.sealant.compiler.addContributesToAnnotation
import dev.steinerok.sealant.compiler.generateSimpleNameString
import dev.steinerok.sealant.compiler.ksp.SealantFileSpec
import dev.steinerok.sealant.compiler.ksp.findScopesForSealantFeatureIntegration
import dev.steinerok.sealant.compiler.ksp.getSymbolsWithAnnotation
import dev.steinerok.sealant.compiler.ksp.requireContainingFile
import dev.steinerok.sealant.compiler.ksp.simpleValidatePredicate

/**
 * Generates scope-level injector infrastructure for Sealant app-component integration.
 *
 * The emitted types expose empty-safe multibinding maps and the owner interface through which
 * Android entry points can later resolve their generated injectors.
 */
public class AppComponentIntegrationSymbolProcessor(
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
                    .findScopesForSealantFeatureIntegration(SealantFeature.AppComponent)
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
            // Генерируем Integrative Multibinds Module
            addType(buildIntegrativeModule(scopeClassName, scopeClassNameStr, fileNode))

            // Генерируем Injectors Owner Interface
            addType(buildInjectorsOwner(scopeClassName, scopeClassNameStr, fileNode))
        }
    }

    /**
     * Generates the Integrative Multibinds Module.
     * * Declares Metro multibinding maps for standard Android components (Activities,
     * BroadcastReceivers, ContentProviders, and Services). Using `@Multibinds` ensures
     * that the Metro graph compiles successfully even if some of these maps are currently
     * empty (i.e., no specific bindings have been contributed to them yet).
     * * Output example:
     * ```kotlin
     * @BindingContainer
     * @ContributesTo(scope = <Scope>::class)
     * public interface <Scope>_SealantAppcomponent_IntegrativeModule {
     *     @Multibinds(allowEmpty = true)
     *     public fun activityInjectors(): SealantActivityInjectorsMap
     *
     *     @Multibinds(allowEmpty = true)
     *     public fun broadcastReceiverInjectors(): BroadcastReceiverInjectorsMap
     *
     *     @Multibinds(allowEmpty = true)
     *     public fun contentProviderInjectors(): SealantContentProviderInjectorsMap
     *
     *     @Multibinds(allowEmpty = true)
     *     public fun serviceInjectors(): SealantServiceInjectorsMap
     * }
     * ```
     */
    private fun buildIntegrativeModule(
        scopeClassName: ClassName,
        scopeClassNameStr: String,
        fileNode: KSFile
    ): TypeSpec {
        val imNameStr = "${scopeClassNameStr}_${featureName}_IntegrativeModule"
        val imClassName = ClassName(integrationPkg, imNameStr)

        return InterfaceSpec(imClassName) {
            addAnnotation(ClassNames.bindingContainer)
            addContributesToAnnotation(scopeClassName)

            addFunction(FunSpec("activityInjectors") {
                addAnnotation(AnnotationSpec(ClassNames.multibinds) {
                    addMember("allowEmpty = true")
                })
                addModifiers(KModifier.ABSTRACT)
                returns(ClassNames.sealantActivityInjectorsMap)
            })
            addFunction(FunSpec("broadcastReceiverInjectors") {
                addAnnotation(AnnotationSpec(ClassNames.multibinds) {
                    addMember("allowEmpty = true")
                })
                addModifiers(KModifier.ABSTRACT)
                returns(ClassNames.sealantBroadcastReceiverInjectorsMap)
            })
            addFunction(FunSpec("contentProviderInjectors") {
                addAnnotation(AnnotationSpec(ClassNames.multibinds) {
                    addMember("allowEmpty = true")
                })
                addModifiers(KModifier.ABSTRACT)
                returns(ClassNames.sealantContentProviderInjectorsMap)
            })
            addFunction(FunSpec("serviceInjectors") {
                addAnnotation(AnnotationSpec(ClassNames.multibinds) {
                    addMember("allowEmpty = true")
                })
                addModifiers(KModifier.ABSTRACT)
                returns(ClassNames.sealantServiceInjectorsMap)
            })

            addOriginatingKSFile(fileNode)
        }
    }

    /**
     * Generates the Injectors Owner Interface.
     * * Contributes the `SealantInjectorsOwner` interface to the target `<Scope>`.
     * This ensures that the component owning this scope (typically the Application component)
     * exposes the necessary injector maps. This allows the dependency dispatcher to retrieve
     * and execute the correct injector for a given Android component at runtime.
     * * Output example:
     * ```kotlin
     * @ContributesTo(scope = <Scope>::class)
     * public interface <Scope>_SealantInjectorsOwner : SealantInjectorsOwner
     * ```
     */
    private fun buildInjectorsOwner(
        scopeClassName: ClassName,
        scopeClassNameStr: String,
        fileNode: KSFile,
    ): TypeSpec {
        val ioSnStr = ClassNames.sealantInjectorsOwner.generateSimpleNameString()
        val ioClassName = ClassName(integrationPkg, "${scopeClassNameStr}_$ioSnStr")

        return InterfaceSpec(ioClassName) {
            addSuperinterface(ClassNames.sealantInjectorsOwner)
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
            return AppComponentIntegrationSymbolProcessor(
                codeGenerator = environment.codeGenerator,
                options = environment.options,
                logger = environment.logger,
            )
        }
    }
}
