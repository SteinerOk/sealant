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
import dev.steinerok.sealant.compiler.ObjectSpec
import dev.steinerok.sealant.compiler.ParameterSpec
import dev.steinerok.sealant.compiler.SealantFeature
import dev.steinerok.sealant.compiler.addContributesToAnnotation
import dev.steinerok.sealant.compiler.buildVmScopeClassName
import dev.steinerok.sealant.compiler.ksp.SealantFileSpec
import dev.steinerok.sealant.compiler.ksp.getSymbolsWithAnnotation
import dev.steinerok.sealant.compiler.ksp.hasSealantFeatureForScope
import dev.steinerok.sealant.compiler.ksp.implements
import dev.steinerok.sealant.compiler.ksp.requireContainingFile
import dev.steinerok.sealant.compiler.ksp.scope

/**
 * Source generator to support Sealant injection of ViewModels.
 *
 * Should generate:
 * ```
 * @Module
 * @ContributesTo(scope = <Scope>::class)
 * public object <ViewModel>_KeyModule {
 *   @Provides
 *   @IntoSet
 *   @SealantViewModelMap.KeySet
 *   public fun provide<ViewModel>Key(): Class<out ViewModel> = <ViewModel>::class.java
 * }
 *
 * @Module
 * @ContributesTo(scope = ViewModel_<Scope>::class)
 * public interface <ViewModel>_BindsModule {
 *   @Binds
 *   @IntoMap
 *   @ViewModelKey(<ViewModel>::class)
 *   @SealantViewModelMap
 *   public fun bind(instance: <ViewModel>): ViewModel
 * }
 * ```
 *
 * related with Hilt codegen:
 * ```
 * public final class $_HiltModules {
 *   @Module
 *   @InstallIn(ViewModelComponent.class)
 *   public static abstract class BindsModule {
 *     @Binds
 *     @IntoMap
 *     @StringKey("pkg.$")
 *     @HiltViewModelMap
 *     public abstract ViewModel bind($ vm)
 *   }
 *
 *   @Module
 *   @InstallIn(ActivityRetainedComponent.class)
 *   public static final class KeyModule {
 *     @Provides
 *     @IntoSet
 *     @HiltViewModelMap.KeySet
 *     public static String provide() {
 *      return "pkg.$";
 *     }
 *   }
 * }
 * ```
 */
public class ViewModelCreationSymbolProcessor(
    private val codeGenerator: CodeGenerator,
    @Suppress("unused") private val options: Map<String, String>,
    @Suppress("unused") private val logger: KSPLogger,
) : SymbolProcessor {

    override fun process(resolver: Resolver): List<KSAnnotated> {
        resolver
            .getSymbolsWithAnnotation(ClassNames.contributesViewModel)
            .filterIsInstance<KSClassDeclaration>()
            .filter { annotated ->
                annotated
                    .scope()
                    .hasSealantFeatureForScope(SealantFeature.ViewModel)
            }
            .onEach { clazz ->
                /* Verification if you need */
                if (!clazz.implements(ClassNames.androidxViewModel)) {
                    logger.error(
                        message = "The annotation `@SealantViewModel` can only be applied " +
                                "to classes which extend ${ClassNames.androidxViewModel}",
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
            val vmScopeClassName = buildVmScopeClassName(scopeClassName)
            //
            val kmNameStr = "${origShortName}_KeyModule"
            val kmClassName = ClassName(packageName, kmNameStr)
            val kmObject = ObjectSpec(kmClassName) {
                addAnnotation(ClassNames.module)
                addContributesToAnnotation(scopeClassName)
                addFunction(
                    FunSpec("provide${origShortName}Key") {
                        addAnnotation(ClassNames.provides)
                        addAnnotation(ClassNames.intoSet)
                        addAnnotation(ClassNames.sealantViewModelMapKeySet)
                        returns(ClassNames.javaClazzOutViewModel)
                        addStatement("return·%T::class.java", origClassName)
                    }
                )
                addOriginatingKSFile(clazz.requireContainingFile())
            }
            addType(kmObject)
            //
            val bmNameStr = "${origShortName}_BindsModule"
            val bmClassName = ClassName(packageName, bmNameStr)
            val bmInterface = InterfaceSpec(bmClassName) {
                addAnnotation(ClassNames.module)
                addContributesToAnnotation(vmScopeClassName)
                addFunction(
                    FunSpec("bind") {
                        addAnnotation(ClassNames.binds)
                        addAnnotation(ClassNames.intoMap)
                        addAnnotation(
                            AnnotationSpec(ClassNames.viewModelKey) {
                                addMember("%T::class", origClassName)
                            }
                        )
                        addAnnotation(ClassNames.sealantViewModelMap)
                        addModifiers(KModifier.ABSTRACT)
                        addParameter(ParameterSpec("instance", origClassName))
                        returns(ClassNames.androidxViewModel)
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
            return ViewModelCreationSymbolProcessor(
                codeGenerator = environment.codeGenerator,
                options = environment.options,
                logger = environment.logger,
            )
        }
    }
}
