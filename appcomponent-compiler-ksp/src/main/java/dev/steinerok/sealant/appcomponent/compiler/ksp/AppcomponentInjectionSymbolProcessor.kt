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
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.STAR
import com.squareup.kotlinpoet.ksp.addOriginatingKSFile
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.writeTo
import dev.steinerok.sealant.compiler.AnnotationSpec
import dev.steinerok.sealant.compiler.ClassNames
import dev.steinerok.sealant.compiler.ClassSpec
import dev.steinerok.sealant.compiler.FunSpec
import dev.steinerok.sealant.compiler.InterfaceSpec
import dev.steinerok.sealant.compiler.ParameterSpec
import dev.steinerok.sealant.compiler.PropertySpec
import dev.steinerok.sealant.compiler.SealantFeature
import dev.steinerok.sealant.compiler.addContributesToAnnotation
import dev.steinerok.sealant.compiler.addPrimaryConstructorAndProperties
import dev.steinerok.sealant.compiler.ksp.SealantFileSpec
import dev.steinerok.sealant.compiler.ksp.getSymbolsWithAnnotation
import dev.steinerok.sealant.compiler.ksp.hasSealantFeatureForScope
import dev.steinerok.sealant.compiler.ksp.implements
import dev.steinerok.sealant.compiler.ksp.requireContainingFile
import dev.steinerok.sealant.compiler.ksp.scope

/**
 * Should generate:
 * ```
 * public class <Type>_SealantInjector @Inject constructor(
 *     public override val injector: MembersInjector<<Type>>
 * ) : SealantInjector<<Type>>
 *
 * @Module
 * @ContributesTo(scope = <Scope>::class)
 * public interface <Type>_SealantInjector_BindsModule {
 *     @Binds
 *     @IntoMap
 *     @ActivityKey(<Type>::class)
 *     public fun bind(instance: <Type>_SealantInjector): AnvilInjector<*>
 * }
 * ```
 */
public class AppcomponentInjectionSymbolProcessor(
    private val codeGenerator: CodeGenerator,
    @Suppress("unused") private val options: Map<String, String>,
    @Suppress("unused") private val logger: KSPLogger,
) : SymbolProcessor {

    override fun process(resolver: Resolver): List<KSAnnotated> {
        resolver
            .getSymbolsWithAnnotation(ClassNames.injectWith)
            .filterIsInstance<KSClassDeclaration>()
            .filter { annotated ->
                annotated
                    .scope()
                    .hasSealantFeatureForScope(SealantFeature.Appcomponent)
            }
            .onEach { _ ->  /* Verification if you need */ }
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
        val fileName = clazz.simpleName.asString() + "_Injection"
        //
        val content = SealantFileSpec(packageName, fileName) {
            val origClassName = clazz.toClassName()
            val origShortName = clazz.simpleName.asString()
            val scopeClassName = clazz.scope().toClassName()
            val injectorNameStr = "${origShortName}_${ClassNames.sealantInjector.simpleName}"
            //
            val injectorClassName = ClassName(packageName, injectorNameStr)
            val injectorClass = ClassSpec(injectorClassName) {
                addSuperinterface(ClassNames.sealantInjector.parameterizedBy(origClassName))
                val miType = ClassNames.membersInjector.parameterizedBy(origClassName)
                addPrimaryConstructorAndProperties(
                    PropertySpec("injector", miType) {
                        addModifiers(KModifier.OVERRIDE)
                    }
                )
                addOriginatingKSFile(clazz.requireContainingFile())
            }
            addType(injectorClass)
            //
            val ibmNameStr = "${injectorNameStr}_BindsModule"
            val ibmClassName = ClassName(packageName, ibmNameStr)
            val ibmInterface = InterfaceSpec(ibmClassName) {
                addAnnotation(ClassNames.module)
                addContributesToAnnotation(scopeClassName)
                addFunction(
                    FunSpec("bind") {
                        addAnnotation(ClassNames.binds)
                        addAnnotation(ClassNames.intoMap)
                        addAnnotation(
                            if (clazz.implements(ClassNames.androidxActivity)) {
                                AnnotationSpec(ClassNames.activityKey) {
                                    addMember("%T::class", origClassName)
                                }
                            } else {
                                AnnotationSpec(ClassNames.classKey) {
                                    addMember("%T::class", origClassName)
                                }
                            }
                        )
                        addModifiers(KModifier.ABSTRACT)
                        addParameter(ParameterSpec("instance", injectorClassName))
                        returns(ClassNames.sealantInjector.parameterizedBy(STAR))
                    }
                )
                addOriginatingKSFile(clazz.requireContainingFile())
            }
            addType(ibmInterface)
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
            return AppcomponentInjectionSymbolProcessor(
                codeGenerator = environment.codeGenerator,
                options = environment.options,
                logger = environment.logger,
            )
        }
    }
}
