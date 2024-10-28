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
import com.google.devtools.ksp.symbol.KSType
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.KModifier
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
 * Should generate:
 * ```
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
public class FragmentCreationSymbolProcessor(
    private val codeGenerator: CodeGenerator,
    @Suppress("unused") private val options: Map<String, String>,
    @Suppress("unused") private val logger: KSPLogger,
) : SymbolProcessor {

    override fun process(resolver: Resolver): List<KSAnnotated> {
        resolver
            .getSymbolsWithAnnotation(ClassNames.contributesFragment)
            .filterIsInstance<KSClassDeclaration>()
            .filter { annotated ->
                annotated
                    .scope()
                    .hasSealantFeatureForScope(SealantFeature.Fragment)
            }
            .onEach { clazz ->
                /* Verification if you need */
                if (!clazz.implements(ClassNames.androidxFragment)) {
                    logger.error(
                        message = "The annotation `@ContributesFragment` can only be applied " +
                                "to classes which extend ${ClassNames.androidxFragment}",
                        symbol = clazz
                    )
                }
            }
            .forEach { symbol ->
                generateByProcessor(symbol).writeTo(
                    codeGenerator = codeGenerator,
                    aggregating = false,
                )
            }
        return emptyList()
    }

    private fun generateByProcessor(clazz: KSClassDeclaration): FileSpec {
        val packageName = clazz.packageName.asString()
        val fileName = clazz.simpleName.asString() + "_Creation"
        //
        val content = SealantFileSpec(packageName, fileName) {
            val origClassName = clazz.toClassName()
            val origShortName = clazz.simpleName.asString()
            val scopeClassName = clazz.scope().toClassName()
            //
            val bmNameStr = "${origShortName}_BindsModule"
            val bmClassName = ClassName(packageName, bmNameStr)
            val bmInterface = InterfaceSpec(bmClassName) {
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
                        addMember("replaces·=·$replacesStr", *replaces.toTypedArray())
                    }
                }
                addFunction(
                    FunSpec("bind") {
                        addAnnotation(ClassNames.binds)
                        addAnnotation(ClassNames.intoMap)
                        addAnnotation(
                            AnnotationSpec(ClassNames.fragmentKey) {
                                addMember("%T::class", origClassName)
                            }
                        )
                        addModifiers(KModifier.ABSTRACT)
                        addParameter(ParameterSpec("instance", origClassName))
                        returns(ClassNames.androidxFragment)
                    }
                )
                addOriginatingKSFile(clazz.requireContainingFile())
            }
            addType(bmInterface)
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
            return FragmentCreationSymbolProcessor(
                codeGenerator = environment.codeGenerator,
                options = environment.options,
                logger = environment.logger,
            )
        }
    }
}
