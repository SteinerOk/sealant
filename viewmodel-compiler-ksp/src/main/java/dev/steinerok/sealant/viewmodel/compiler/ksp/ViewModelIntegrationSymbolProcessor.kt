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
import com.squareup.kotlinpoet.MAP
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.STRING
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
import dev.steinerok.sealant.compiler.generateSimpleNameString
import dev.steinerok.sealant.compiler.ksp.SealantFileSpec
import dev.steinerok.sealant.compiler.ksp.SealantOptions
import dev.steinerok.sealant.compiler.ksp.findScopesForSealantFeatureIntegration
import dev.steinerok.sealant.compiler.ksp.getSymbolsWithAnnotation
import dev.steinerok.sealant.compiler.ksp.requireContainingFile

/**
 * Description of the ViewModel Subcomponent and factory infrastructure generation.
 * This generator creates the core Dagger architecture required to instantiate ViewModels
 * that depend on a `SavedStateHandle`. It achieves this by generating a dedicated
 * subcomponent for the ViewModel scope, allowing the state handle to be bound at runtime.
 *
 * Should generate the following components:
 *
 * 1. ViewModel Subcomponent & Factory:
 * Creates an Anvil `@MergeSubcomponent` tied to `ViewModel_<Scope>`. This acts as an
 * isolated dependency graph specifically for ViewModels. The `@Subcomponent.Factory`
 * forces the provision of a `SavedStateHandle` via `@BindsInstance`, making it available
 * to any ViewModel created within this subcomponent. The `Parent` interface allows the
 * parent component to explicitly expose the subcomponent's dependencies.
 * ```
 * @SingleIn(SealantViewModelScope::class)
 * @MergeSubcomponent(scope = ViewModel_<Scope>::class)
 * public interface <Scope>_SealantViewModelSubcomponent : SealantViewModelSubcomponent {
 *
 *     @ContributesTo(scope = <Scope>::class)   // NOTE: Only in Metro
 *     @Subcomponent.Factory
 *     public interface Factory : SealantViewModelSubcomponent.Factory {
 *         public override fun create(@BindsInstance ssHandle: SavedStateHandle): <Scope>_SealantViewModelSubcomponent
 *     }
 *
 *     @ContributesTo(scope = <Scope>::class)
 *     public interface Parent : SealantViewModelSubcomponent.Parent
 * }
 * ```
 *
 * 2. Main Scope Integrative Module:
 * Contributes to the parent `<Scope>`. It includes the newly generated subcomponent
 * and declares foundational multibinding maps: one for registering supported ViewModel
 * classes (`KeySet`), and another for aggregating subcomponent factories.
 * ```
 * @Module(subcomponents = [<Scope>_SealantViewModelSubcomponent::class])
 * @ContributesTo(scope = <Scope>::class)
 * public interface <Scope>_SealantViewModelSubcomponent_IntegrativeModule {
 *
 *     @Multibinds
 *     @SealantViewModelMap.KeySet
 *     public fun bindVmClassSet(): Set<Class<out ViewModel>>
 *
 *     @Multibinds
 *     @SealantViewModelSupport.SubcomponentMap
 *     public fun bindVmSubcomponentFactoryMap(): Map<String, SealantViewModelSubcomponent.Factory>
 * }
 * ```
 *
 * 3. Subcomponent Factory Binds Module:
 * Contributes to the parent `<Scope>`. It explicitly binds the generated Subcomponent
 * Factory into the multibinding map defined above, using the scope's package string as a key.
 * This allows the parent factory creator to locate and use this specific subcomponent factory.
 * ```
 * @Module
 * @ContributesTo(scope = <Scope>::class)
 * public interface <Scope>_SealantViewModelSubcomponent_BindsModule {
 *
 *     @Binds
 *     @IntoMap
 *     @StringKey("scope_pkg.<Scope>")
 *     @SealantViewModelSupport.SubcomponentMap
 *     public fun bind(instance: <Scope>_SealantViewModelSubcomponent.Factory): SealantViewModelSubcomponent.Factory
 * }
 * ```
 *
 * 4. ViewModel Factory Creator Owner:
 * Exposes the `SealantViewModelFactoryCreator` to the parent `<Scope>`, ensuring the
 * system can access the mechanism required to spin up the ViewModel subcomponents.
 * ```
 * @ContributesTo(scope = <Scope>::class)
 * public interface <Scope>_SealantViewModelFactoryCreatorOwner : SealantViewModelFactoryCreator.Owner
 * ```
 *
 * 5. ViewModel Scope Integrative Module:
 * Contributes to the `ViewModel_<Scope>`. It defines the Multibinding map where all
 * the actual `ViewModel` instances will be bound (using the `@SealantViewModelMap` qualifier).
 * ```
 * @Module
 * @ContributesTo(scope = ViewModel_<Scope>::class)
 * public interface <Scope>_ViewModelFactories_IntegrativeModule {
 *
 *     @Multibinds
 *     @SealantViewModelMap
 *     public fun bindWmMap(): Map<Class<out ViewModel>, ViewModel>
 * }
 * ```
 *
 * 6. ViewModel Factories Owner:
 * Contributes to the `ViewModel_<Scope>`. This ensures the subcomponent officially exposes
 * the fully constructed `ViewModelFactories`, allowing the Android framework to finally
 * retrieve the instantiated ViewModels.
 * ```
 * @ContributesTo(scope = ViewModel_<Scope>::class)
 * public interface <Scope>_ViewModelFactoriesOwner : ViewModelFactoriesOwner
 * ```
 *
 * 7. !!! Only in Metro !!! ViewModel Factory Creator Provides Module:
 * Contributes a singleton object module to the `<Scope>`. It provides the
 * `SealantViewModelFactoryCreator` as a scoped instance (`@SingleIn`). To construct
 * this creator, Metro injects the Android `Application` context alongside the
 * heavily aggregated Multibinding collections generated in previous steps: the set
 * of supported ViewModel classes (`vmKeySet`) and the map of subcomponent factories
 * (`vmSubcomponentFactoryMap`). This creator ultimately generates the
 * `ViewModelProvider.Factory` used by the UI layer.
 * ```
 * @ContributesTo(scope = AppScope::class)
 * @Module
 * public object AppScope_SealantViewModelSubcomponent_ProvidesModule {
 *     @Provides
 *     @SingleIn(AppScope::class)
 *     public fun provideCreator(
 *         application: Application,
 *         @SealantViewModelSupport.KeySet vmKeySet: Set<Class<out ViewModel>>,
 *         @SealantViewModelSupport.SubcomponentMap vmSubcomponentFactoryMap: Map<String, Provider<SealantViewModelSubcomponent.Factory>>
 *     ): SealantViewModelFactoryCreator = SealantViewModelFactoryCreator(application, vmKeySet, vmSubcomponentFactoryMap)
 * }
 * ```
 */
public class ViewModelIntegrationSymbolProcessor(
    private val codeGenerator: CodeGenerator,
    options: Map<String, String>,
    private val logger: KSPLogger,
) : SymbolProcessor {

    private val options = SealantOptions.load(options, logger)

    override fun process(resolver: Resolver): List<KSAnnotated> {
        resolver
            .getSymbolsWithAnnotation(ClassNames.sealantIntegration)
            .filterIsInstance<KSClassDeclaration>()
            .flatMap { annotated ->
                annotated
                    .findScopesForSealantFeatureIntegration(SealantFeature.ViewModel)
                    .map { annotated to it }
            }
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
            val mvmsNameStr = if (options.useMetro) vmsNameStr else "Merged$vmsNameStr"
            val mvmsClassName = ClassName(packageName, mvmsNameStr)
            val vmsInterface = InterfaceSpec(vmsClassName) {
                addSuperinterface(ClassNames.sealantViewModelSubcomponent)
                addAnnotation(
                    AnnotationSpec(ClassNames.singleIn) {
                        addMember("scope·=·%T::class", ClassNames.sealantViewModelScope)
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
                        if (options.useMetro) addContributesToAnnotation(scopeClassName)
                        addAnnotation(ClassNames.mergeSubcomponentFactory)
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
                        addAnnotation(ClassNames.sealantViewModelSupportKeySet)
                        addModifiers(KModifier.ABSTRACT)
                        returns(ClassNames.viewModelClassSet)
                    }
                )
                addFunction(
                    FunSpec("bindVmSubcomponentFactoryMap") {
                        addAnnotation(ClassNames.multibinds)
                        addAnnotation(ClassNames.sealantViewModelSupportSubcomponentMap)
                        addModifiers(KModifier.ABSTRACT)
                        returns(ClassNames.sealantViewModelSubcomponentFactoryMap)
                    }
                )
                addOriginatingKSFile(clazz.requireContainingFile())
            }
            addType(imInterface)
            //
            val bmNameStr =
                "${scopeClassNameStr}_${ClassNames.sealantViewModelSubcomponent.simpleName}_BindsModule"
            val bmClassName = ClassName(packageName, bmNameStr)
            val bmInterface = InterfaceSpec(bmClassName) {
                addContributesToAnnotation(scopeClassName)
                addAnnotation(ClassNames.module)
                addFunction(
                    FunSpec("bind") {
                        addAnnotation(ClassNames.binds)
                        addAnnotation(ClassNames.intoMap)
                        addAnnotation(
                            AnnotationSpec(ClassNames.stringKey) {
                                addMember("%S", scopeClassName.reflectionName().replace("..", "."))
                            }
                        )
                        addAnnotation(ClassNames.sealantViewModelSupportSubcomponentMap)
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
            addType(bmInterface)
            //
            if (options.useMetro) {
                val pmNameStr =
                    "${scopeClassNameStr}_${ClassNames.sealantViewModelSubcomponent.simpleName}_ProvidesModule"
                val pmClassName = ClassName(packageName, pmNameStr)
                val pmObject = ObjectSpec(pmClassName) {
                    addContributesToAnnotation(scopeClassName)
                    addAnnotation(ClassNames.module)
                    addFunction(
                        FunSpec("provideCreator") {
                            addAnnotation(ClassNames.provides)
                            addAnnotation(
                                AnnotationSpec(ClassNames.singleIn) {
                                    addMember("%T::class", scopeClassName)
                                }
                            )
                            addParameter(
                                ParameterSpec("application", ClassNames.androidApplication)
                            )
                            addParameter(
                                ParameterSpec("vmKeySet", ClassNames.viewModelClassSet) {
                                    addAnnotation(
                                        ClassName(
                                            "dev.steinerok.sealant.viewmodel",
                                            "SealantViewModelSupport", "KeySet"
                                        )
                                    )
                                }
                            )
                            addParameter(
                                ParameterSpec(
                                    "vmSubcomponentFactoryMap",
                                    MAP.parameterizedBy(
                                        STRING,
                                        ClassNames.provider.parameterizedBy(
                                            ClassNames.sealantViewModelSubcomponentFactory
                                        )
                                    )
                                ) {
                                    addAnnotation(
                                        ClassName(
                                            "dev.steinerok.sealant.viewmodel",
                                            "SealantViewModelSupport", "SubcomponentMap"
                                        )
                                    )
                                }
                            )
                            returns(ClassNames.sealantViewModelFactoryCreator)
                            addStatement(
                                "return %T(application, vmKeySet, vmSubcomponentFactoryMap)",
                                ClassNames.sealantViewModelFactoryCreator
                            )
                        }
                    )
                    addOriginatingKSFile(clazz.requireContainingFile())
                }
                addType(pmObject)
            }
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
