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
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ksp.addOriginatingKSFile
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.writeTo
import dev.steinerok.sealant.compiler.ClassNames
import dev.steinerok.sealant.compiler.FunSpec
import dev.steinerok.sealant.compiler.InterfaceSpec
import dev.steinerok.sealant.compiler.SealantFeature
import dev.steinerok.sealant.compiler.addContributesToAnnotation
import dev.steinerok.sealant.compiler.generateSimpleNameString
import dev.steinerok.sealant.compiler.ksp.SealantFileSpec
import dev.steinerok.sealant.compiler.ksp.findScopesForSealantFeatureIntegration
import dev.steinerok.sealant.compiler.ksp.getSymbolsWithAnnotation
import dev.steinerok.sealant.compiler.ksp.requireContainingFile

/**
 * Should generate:
 * ```
 * @Module
 * @ContributesTo(scope = <Scope>::class)
 * public interface <Scope>_SealantAppcomponent_IntegrativeModule {
 *     @Multibinds
 *     public fun activityInjectors(): SealantActivityInjectorsMap
 *
 *     @Multibinds
 *     public fun otherInjectors(): SealantOtherInjectorsMap
 * }
 *
 * @ContributesTo(scope = <Scope>::class)
 * public interface <Scope>_SealantInjectorsOwner : SealantInjectorsOwner
 * ```
 */
public class AppcomponentIntegrationSymbolProcessor(
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
                    .findScopesForSealantFeatureIntegration(SealantFeature.Appcomponent)
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
        scope: KSClassDeclaration,
    ): FileSpec {
        val packageName = integrationPkg
        val scopeClassName = scope.toClassName()
        val scopeClassNameStr = scopeClassName.generateSimpleNameString()
        val fileName = "${scopeClassNameStr}_${featureName}_Integration"
        //
        val content = SealantFileSpec(packageName, fileName) {
            //
            val imNameStr = "${scopeClassNameStr}_${featureName}_IntegrativeModule"
            val imClassName = ClassName(packageName, imNameStr)
            val imInterface = InterfaceSpec(imClassName) {
                addAnnotation(ClassNames.module)
                addContributesToAnnotation(scopeClassName)
                addFunction(
                    FunSpec("activityInjectors") {
                        addAnnotation(ClassNames.multibinds)
                        addModifiers(KModifier.ABSTRACT)
                        returns(ClassNames.sealantActivityInjectorsMap)
                    }
                )
                addFunction(
                    FunSpec("otherInjectors") {
                        addAnnotation(ClassNames.multibinds)
                        addModifiers(KModifier.ABSTRACT)
                        returns(ClassNames.sealantOtherInjectorsMap)
                    }
                )
                addOriginatingKSFile(clazz.requireContainingFile())
            }
            addType(imInterface)
            //
            val ioNameStr =
                "${scopeClassNameStr}_${ClassNames.sealantInjectorsOwner.generateSimpleNameString()}"
            val ioClassName = ClassName(packageName, ioNameStr)
            val ioInterface = InterfaceSpec(ioClassName) {
                addSuperinterface(ClassNames.sealantInjectorsOwner)
                addContributesToAnnotation(scopeClassName)
                addOriginatingKSFile(clazz.requireContainingFile())
            }
            addType(ioInterface)
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
            return AppcomponentIntegrationSymbolProcessor(
                codeGenerator = environment.codeGenerator,
                options = environment.options,
                logger = environment.logger,
            )
        }
    }
}