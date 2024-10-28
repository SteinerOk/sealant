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
import dev.steinerok.sealant.compiler.ParameterSpec
import dev.steinerok.sealant.compiler.SealantFeature
import dev.steinerok.sealant.compiler.addContributesToAnnotation
import dev.steinerok.sealant.compiler.buildVmScopeClassName
import dev.steinerok.sealant.compiler.generateSimpleNameString
import dev.steinerok.sealant.compiler.ksp.SealantFileSpec
import dev.steinerok.sealant.compiler.ksp.findScopesForSealantFeatureIntegration
import dev.steinerok.sealant.compiler.ksp.getSymbolsWithAnnotation
import dev.steinerok.sealant.compiler.ksp.requireContainingFile

/**
 * Should generate:
 * ```
 * @SingleIn(SealantViewModelScope::class)
 * @MergeSubcomponent(scope = ViewModel_<Scope>::class)
 * public interface <Scope>_SealantViewModelSubcomponent : SealantViewModelSubcomponent {
 *
 *     @Subcomponent.Factory
 *     public interface Factory : SealantViewModelSubcomponent.Factory {
 *         public override fun create(@BindsInstance ssHandle: SavedStateHandle): <Scope>_SealantViewModelSubcomponent
 *     }
 *
 *     @ContributesTo(scope = <Scope>::class)
 *     public interface Parent : SealantViewModelSubcomponent.Parent
 * }
 *
 * @Module(subcomponents = [<Scope>_SealantViewModelSubcomponent::class])
 * @ContributesTo(scope = <Scope>::class)
 * public interface <Scope>_SealantViewModelSubcomponent_IntegrativeModule {
 *
 *     @Multibinds
 *     @SealantViewModelMap.KeySet
 *     public fun bindVmClassSet(): Set<Class<out ViewModel>>
 *
 *     @Binds
 *     @IntoMap
 *     @StringKey("scope_pkg.<Scope>")
 *     @SealantViewModelMap.SubcomponentMap
 *     public fun bind(instance: <Scope>_SealantViewModelSubcomponent.Factory): SealantViewModelSubcomponent.Factory
 * }
 *
 * @ContributesTo(scope = <Scope>::class)
 * public interface <Scope>_SealantViewModelFactoryCreatorOwner : SealantViewModelFactoryCreator.Owner
 *
 * @Module
 * @ContributesTo(scope = ViewModel_<Scope>::class)
 * public interface <Scope>_ViewModelFactories_IntegrativeModule {
 *
 *     @Multibinds
 *     @SealantViewModelMap
 *     public fun bindWmMap(): Map<Class<out ViewModel>, ViewModel>
 * }
 *
 * @ContributesTo(scope = ViewModel_<Scope>::class)
 * public interface <Scope>_ViewModelFactoriesOwner : ViewModelFactoriesOwner
 * ```
 */
public class ViewModelIntegrationSymbolProcessor(
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
                    .findScopesForSealantFeatureIntegration(SealantFeature.ViewModel)
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
            val vmScopeClassName = buildVmScopeClassName(scopeClassName)
            //
            val vmsNameStr =
                "${scopeClassNameStr}_${ClassNames.sealantViewModelSubcomponent.simpleName}"
            val vmsClassName = ClassName(packageName, vmsNameStr)
            // Required via https://github.com/ZacSweers/anvil/blob/main/FORK.md#subcomponents
            val mvmsNameStr = "Merged$vmsNameStr"
            val mvmsClassName = ClassName(packageName, mvmsNameStr)
            val vmsInterface = InterfaceSpec(vmsClassName) {
                addSuperinterface(ClassNames.sealantViewModelSubcomponent)
                addAnnotation(
                    AnnotationSpec(ClassNames.singleIn) {
                        addMember("%T::class", ClassNames.sealantViewModelScope)
                    }
                )
                addAnnotation(
                    AnnotationSpec(ClassNames.mergeSubcomponent) {
                        addMember("scope·=·%T::class", vmScopeClassName)
                    }
                )
                addType(
                    InterfaceSpec("Factory") {
                        addSuperinterface(ClassNames.sealantViewModelSubcomponentFactory)
                        addAnnotation(ClassNames.subcomponentFactory)
                        addFunction(
                            FunSpec("create") {
                                addModifiers(KModifier.ABSTRACT, KModifier.OVERRIDE)
                                addParameter(
                                    ParameterSpec("ssHandle", ClassNames.ssHandle) {
                                        addAnnotation(ClassNames.bindsInstance)
                                    }
                                )
                                returns(vmsClassName)
                            }
                        )
                        addOriginatingKSFile(clazz.requireContainingFile())
                    }
                )
                addType(
                    InterfaceSpec("Parent") {
                        addSuperinterface(ClassNames.sealantViewModelSubcomponentParent)
                        addContributesToAnnotation(scopeClassName)
                        addOriginatingKSFile(clazz.requireContainingFile())
                    }
                )
                addOriginatingKSFile(clazz.requireContainingFile())
            }
            addType(vmsInterface)
            //
            val imNameStr =
                "${scopeClassNameStr}_${ClassNames.sealantViewModelSubcomponent.simpleName}_IntegrativeModule"
            val imClassName = ClassName(packageName, imNameStr)
            val imInterface = InterfaceSpec(imClassName) {
                addAnnotation(
                    AnnotationSpec(ClassNames.module) {
                        addMember("subcomponents·=·[%T::class]", mvmsClassName)
                    }
                )
                addContributesToAnnotation(scopeClassName)
                addFunction(
                    FunSpec("bindVmClassSet") {
                        addAnnotation(ClassNames.multibinds)
                        addAnnotation(ClassNames.sealantViewModelMapKeySet)
                        addModifiers(KModifier.ABSTRACT)
                        returns(ClassNames.viewModelClassSet)
                    }
                )
                addFunction(
                    FunSpec("bind") {
                        addAnnotation(ClassNames.binds)
                        addAnnotation(ClassNames.intoMap)
                        addAnnotation(
                            AnnotationSpec(ClassNames.stringKey) {
                                addMember("%S", scopeClassName.reflectionName().replace("..", "."))
                            }
                        )
                        addAnnotation(ClassNames.sealantViewModelMapSubcomponentMap)
                        addModifiers(KModifier.ABSTRACT)
                        addParameter(
                            ParameterSpec(
                                "instance",
                                mvmsClassName.nestedClass("Factory")
                            )
                        )
                        returns(ClassNames.sealantViewModelSubcomponentFactory)
                    }
                )
                addOriginatingKSFile(clazz.requireContainingFile())
            }
            addType(imInterface)
            //
            val wmfcoNameStr =
                "${scopeClassNameStr}_${ClassNames.sealantViewModelFactoryCreatorOwner.generateSimpleNameString()}"
            val wmfcoClassName = ClassName(packageName, wmfcoNameStr)
            val wmfcoInterface = InterfaceSpec(wmfcoClassName) {
                addSuperinterface(ClassNames.sealantViewModelFactoryCreatorOwner)
                addContributesToAnnotation(scopeClassName)
                addOriginatingKSFile(clazz.requireContainingFile())
            }
            addType(wmfcoInterface)
            //
            val vmfimNameStr = "${scopeClassNameStr}_ViewModelFactories_IntegrativeModule"
            val vmfimClassName = ClassName(packageName, vmfimNameStr)
            val vmfimInterface = InterfaceSpec(vmfimClassName) {
                addAnnotation(ClassNames.module)
                addContributesToAnnotation(vmScopeClassName)
                addFunction(
                    FunSpec("bindWmMap") {
                        addAnnotation(ClassNames.multibinds)
                        addAnnotation(ClassNames.sealantViewModelMap)
                        addModifiers(KModifier.ABSTRACT)
                        returns(ClassNames.viewModelMap)
                    }
                )
                addOriginatingKSFile(clazz.requireContainingFile())
            }
            addType(vmfimInterface)
            //
            val vmfoNameStr =
                "${scopeClassNameStr}_${ClassNames.viewModelFactoriesOwner.simpleName}"
            val vmfoClassName = ClassName(packageName, vmfoNameStr)
            val vmfoInterface = InterfaceSpec(vmfoClassName) {
                addSuperinterface(ClassNames.viewModelFactoriesOwner)
                addContributesToAnnotation(vmScopeClassName)
                addOriginatingKSFile(clazz.requireContainingFile())
            }
            addType(vmfoInterface)
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
            return ViewModelIntegrationSymbolProcessor(
                codeGenerator = environment.codeGenerator,
                options = environment.options,
                logger = environment.logger,
            )
        }
    }
}
