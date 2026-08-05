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
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.validate
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.TypeSpec
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
import dev.steinerok.sealant.compiler.ksp.simpleValidatePredicate

/**
 * Generates the shared subcomponent and factory infrastructure behind Sealant ViewModel support.
 *
 * Each enabled scope receives a generated ViewModel subcomponent that accepts runtime state and
 * lifecycle objects, plus the parent-scope bindings needed to discover and create it.
 */
public class ViewModelIntegrationSymbolProcessor(
    private val codeGenerator: CodeGenerator,
    private val options: Map<String, String>,
    private val logger: KSPLogger,
) : SymbolProcessor {

    @Suppress("unused")
    private val sealantOptions = SealantOptions.load(options, logger)

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val (validSymbols, invalidSymbols) = resolver
            .getSymbolsWithAnnotation(ClassNames.sealantIntegration)
            .filterIsInstance<KSClassDeclaration>()
            .partition { symbol -> symbol.validate(simpleValidatePredicate) }

        validSymbols
            .flatMap { annotated ->
                annotated
                    .findScopesForSealantFeatureIntegration(SealantFeature.ViewModel)
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

        val vmScopeClassName = buildVmScopeClassName(scopeClassName)

        val vmsSnStr = ClassNames.sealantViewModelSubcomponent.simpleName
        val vmsNameStr = "${scopeClassNameStr}_$vmsSnStr"
        val vmsClassName = ClassName(integrationPkg, vmsNameStr)

        val fileNode = clazz.requireContainingFile()
        val fileName = "${scopeClassNameStr}_${featureName}_Integration"

        return SealantFileSpec(integrationPkg, fileName) {
            addType(
                buildViewModelSubcomponent(
                    vmsClassName, scopeClassName, vmScopeClassName, fileNode
                )
            )
            addType(
                buildMainIntegrativeModule(
                    scopeClassNameStr, scopeClassName, vmsClassName, fileNode
                )
            )
            addType(
                buildSubcomponentBindsModule(
                    scopeClassNameStr, scopeClassName, vmsClassName, fileNode
                )
            )
            addType(buildCreatorProvidesModule(scopeClassNameStr, scopeClassName, fileNode))

            addType(buildCreatorOwnerInterface(scopeClassNameStr, scopeClassName, fileNode))
            addType(buildVmScopeIntegrativeModule(scopeClassNameStr, vmScopeClassName, fileNode))
            addType(buildVmFactoriesOwner(scopeClassNameStr, vmScopeClassName, fileNode))
        }
    }


    /**
     * Creates a Metro `@GraphExtension` tied to `<Scope>_ViewModel`. This acts as an
     * isolated dependency graph specifically for ViewModels. The `@GraphExtension.Factory`
     * forces the provision of a `SavedStateHandle` and a `ViewModelLifecycle` via `@Provides`
     * factory parameters.
     * * * Output example:
     * ```kotlin
     * @SingleIn(SealantViewModelScope::class)
     * @GraphExtension(scope = <Scope>_ViewModel::class)
     * public interface <Scope>_SealantViewModelSubcomponent : SealantViewModelSubcomponent {
     *
     *     @ContributesTo(scope = <Scope>::class)
     *     @GraphExtension.Factory
     *     public interface Factory : SealantViewModelSubcomponent.Factory {
     *         public override fun create(
     *             @Provides ssHandle: SavedStateHandle,
     *             @Provides vmLifecycle: ViewModelLifecycle,
     *         ): <Scope>_SealantViewModelSubcomponent
     *     }
     *
     *     @ContributesTo(scope = <Scope>::class)
     *     public interface Parent : SealantViewModelSubcomponent.Parent
     * }
     * ```
     */
    private fun buildViewModelSubcomponent(
        vmsClassName: ClassName,
        scopeClassName: ClassName,
        vmScopeClassName: ClassName,
        fileNode: KSFile,
    ): TypeSpec {
        return InterfaceSpec(vmsClassName) {
            addSuperinterface(ClassNames.sealantViewModelSubcomponent)
            addAnnotation(AnnotationSpec(ClassNames.singleIn) {
                addMember("scope = %T::class", ClassNames.sealantViewModelScope)
            })
            addAnnotation(AnnotationSpec(ClassNames.graphExtension) {
                addMember("scope = %T::class", vmScopeClassName)
            })

            addType(InterfaceSpec("Factory") {
                addSuperinterface(ClassNames.sealantViewModelSubcomponentFactory)
                addContributesToAnnotation(scopeClassName)
                addAnnotation(ClassNames.graphExtensionFactory)
                addFunction(FunSpec("create") {
                    addModifiers(KModifier.ABSTRACT, KModifier.OVERRIDE)
                    addParameter(ParameterSpec("ssHandle", ClassNames.androidxSsHandle) {
                        addAnnotation(ClassNames.provides)
                    })
                    addParameter(ParameterSpec("vmLifecycle", ClassNames.viewModelLifecycle) {
                        addAnnotation(ClassNames.provides)
                    })
                    returns(vmsClassName)
                })

                addOriginatingKSFile(fileNode)
            })

            addType(InterfaceSpec("Parent") {
                addSuperinterface(ClassNames.sealantViewModelSubcomponentParent)
                addContributesToAnnotation(scopeClassName)

                addOriginatingKSFile(fileNode)
            })

            addOriginatingKSFile(fileNode)
        }
    }

    /**
     * Contributes to the parent `<Scope>`. It includes the newly generated subcomponent
     * and declares foundational multibinding maps: one for registering supported ViewModel
     * classes (`KeySet`), and another for aggregating subcomponent factories.
     * * * Output example:
     * ```kotlin
     * @BindingContainer
     * @ContributesTo(scope = <Scope>::class)
     * public interface <Scope>_SealantViewModelSubcomponent_IntegrativeModule {
     *
     *     @Multibinds(allowEmpty = true)
     *     @SealantViewModelMap.KeySet
     *     public fun bindVmClassSet(): Set<KClass<out ViewModel>>
     *
     *     @Multibinds(allowEmpty = true)
     *     @SealantViewModelSupport.SubcomponentMap
     *     public fun bindVmSubcomponentFactoryMap(): Map<KClass<out Any>, SealantViewModelSubcomponent.Factory>
     * }
     * ```
     */
    private fun buildMainIntegrativeModule(
        scopeClassNameStr: String,
        scopeClassName: ClassName,
        vmsClassName: ClassName,
        fileNode: KSFile
    ): TypeSpec {
        val vmsSnStr = ClassNames.sealantViewModelSubcomponent.simpleName
        val bmNameStr = "${scopeClassNameStr}_${vmsSnStr}_IntegrativeModule"
        val imClassName = ClassName(integrationPkg, bmNameStr)

        return InterfaceSpec(imClassName) {
            addContributesToAnnotation(scopeClassName)
            addAnnotation(ClassNames.bindingContainer)

            addFunction(FunSpec("bindVmClassSet") {
                addAnnotation(AnnotationSpec(ClassNames.multibinds) {
                    addMember("allowEmpty = true")
                })
                addAnnotation(ClassNames.sealantViewModelSupportKeySet)
                addModifiers(KModifier.ABSTRACT)
                returns(ClassNames.viewModelClassSet)
            })

            addFunction(FunSpec("bindVmSubcomponentFactoryMap") {
                addAnnotation(AnnotationSpec(ClassNames.multibinds) {
                    addMember("allowEmpty = true")
                })
                addAnnotation(ClassNames.sealantViewModelSupportSubcomponentMap)
                addModifiers(KModifier.ABSTRACT)
                returns(ClassNames.sealantViewModelSubcomponentFactoryMap)
            })

            addOriginatingKSFile(fileNode)
        }
    }

    /**
     * Contributes to the parent `<Scope>`. It explicitly binds the generated Subcomponent
     * Factory into the multibinding map defined above, using the scope's package string as a key.
     * * * Output example:
     * ```kotlin
     * @BindingContainer
     * @ContributesTo(scope = <Scope>::class)
     * public interface <Scope>_SealantViewModelSubcomponent_BindsModule {
     *
     *     @Binds
     *     @IntoMap
     *     @SealantViewModelScopeKey(<Scope>::class)
     *     @SealantViewModelSupport.SubcomponentMap
     *     public fun bind(instance: <Scope>_SealantViewModelSubcomponent.Factory): SealantViewModelSubcomponent.Factory
     * }
     * ```
     */
    private fun buildSubcomponentBindsModule(
        scopeClassNameStr: String,
        scopeClassName: ClassName,
        vmsClassName: ClassName,
        fileNode: KSFile,
    ): TypeSpec {
        val vmsSnStr = ClassNames.sealantViewModelSubcomponent.simpleName
        val bmNameStr = "${scopeClassNameStr}_${vmsSnStr}_BindsModule"
        val bmClassName = ClassName(integrationPkg, bmNameStr)

        return InterfaceSpec(bmClassName) {
            addContributesToAnnotation(scopeClassName)
            addAnnotation(ClassNames.bindingContainer)

            addFunction(FunSpec("bind") {
                addAnnotation(ClassNames.binds)
                addAnnotation(ClassNames.intoMap)
                addAnnotation(AnnotationSpec(ClassNames.sealantViewModelScopeKey) {
                    addMember("%T::class", scopeClassName)
                })
                addAnnotation(ClassNames.sealantViewModelSupportSubcomponentMap)
                addModifiers(KModifier.ABSTRACT)
                addParameter(ParameterSpec("instance", vmsClassName.nestedClass("Factory")))
                returns(ClassNames.sealantViewModelSubcomponentFactory)
            })

            addOriginatingKSFile(fileNode)
        }
    }

    /**
     * Contributes a singleton object module to the `<Scope>`. It provides the
     * `SealantViewModelFactoryCreator` as a scoped instance (`@SingleIn`).
     * * * Output example:
     * ```kotlin
     * @ContributesTo(scope = AppScope::class)
     * @BindingContainer
     * public object AppScope_SealantViewModelSubcomponent_ProvidesModule {
     *     @Provides
     *     @SingleIn(AppScope::class)
     *     public fun provideCreator(
     *         application: Application,
     *         @SealantViewModelSupport.KeySet vmKeySet: Set<KClass<out ViewModel>>,
     *         @SealantViewModelSupport.SubcomponentMap vmSubcomponentFactoryMap: Map<KClass<out Any>, () -> SealantViewModelSubcomponent.Factory>
     *     ): SealantViewModelFactoryCreator = SealantViewModelFactoryCreator(application, vmKeySet, vmSubcomponentFactoryMap)
     * }
     * ```
     */
    private fun buildCreatorProvidesModule(
        scopeClassNameStr: String,
        scopeClassName: ClassName,
        fileNode: KSFile
    ): TypeSpec {
        val vmsSnStr = ClassNames.sealantViewModelSubcomponent.simpleName
        val pmSnStr = "${scopeClassNameStr}_${vmsSnStr}_ProvidesModule"
        val pmClassName = ClassName(integrationPkg, pmSnStr)

        return ObjectSpec(pmClassName) {
            addContributesToAnnotation(scopeClassName)
            addAnnotation(ClassNames.bindingContainer)

            addFunction(FunSpec("provideCreator") {
                addAnnotation(ClassNames.provides)
                addAnnotation(AnnotationSpec(ClassNames.singleIn) {
                    addMember("%T::class", scopeClassName)
                })
                addParameter(ParameterSpec("application", ClassNames.androidApplication))
                addParameter(ParameterSpec("vmKeySet", ClassNames.viewModelClassSet) {
                    addAnnotation(ClassNames.sealantViewModelSupportKeySet)
                })
                addParameter(
                    ParameterSpec(
                        "vmSubcomponentFactoryMap",
                        ClassNames.sealantViewModelSubcomponentFactoryProviderMap
                    ) {
                        addAnnotation(ClassNames.sealantViewModelSupportSubcomponentMap)
                    }
                )
                returns(ClassNames.sealantViewModelFactoryCreator)
                addStatement(
                    "return %T(application, vmKeySet, vmSubcomponentFactoryMap)",
                    ClassNames.sealantViewModelFactoryCreator
                )
            })

            addOriginatingKSFile(fileNode)
        }
    }

    /**
     * Exposes the `SealantViewModelFactoryCreator` to the parent `<Scope>`, ensuring the
     * system can access the mechanism required to spin up the ViewModel subcomponents.
     * * * Output example:
     * ```kotlin
     * @ContributesTo(scope = <Scope>::class)
     * public interface <Scope>_SealantViewModelFactoryCreatorOwner : SealantViewModelFactoryCreator.Owner
     * ```
     */
    private fun buildCreatorOwnerInterface(
        scopeClassNameStr: String,
        scopeClassName: ClassName,
        fileNode: KSFile,
    ): TypeSpec {
        val wmfcoSnStr = ClassNames.sealantViewModelFactoryCreatorOwner.generateSimpleNameString()
        val wmfcoClassName = ClassName(integrationPkg, "${scopeClassNameStr}_$wmfcoSnStr")

        return InterfaceSpec(wmfcoClassName) {
            addSuperinterface(ClassNames.sealantViewModelFactoryCreatorOwner)
            addContributesToAnnotation(scopeClassName)

            addOriginatingKSFile(fileNode)
        }
    }

    /**
     * Contributes to the `<Scope>_ViewModel`. It defines the Multibinding map where all
     * the actual `ViewModel` instances will be bound (using the `@SealantViewModelMap` qualifier).
     * * * Output example:
     * ```kotlin
     * @BindingContainer
     * @ContributesTo(scope = <Scope>_ViewModel::class)
     * public interface <Scope>_ViewModelFactories_IntegrativeModule {
     *
     *     @Multibinds(allowEmpty = true)
     *     @SealantViewModelMap
     *     public fun bindWmMap(): Map<KClass<out ViewModel>, ViewModel>
     *
     *     @Multibinds(allowEmpty = true)
     *     @SealantViewModelAssistedMap
     *     public fun bindWmAssistedMap(): Map<KClass<out ViewModel>, Any>
     * }
     * ```
     */
    private fun buildVmScopeIntegrativeModule(
        scopeClassNameStr: String,
        vmScopeClassName: ClassName,
        fileNode: KSFile,
    ): TypeSpec {
        val vmfimNameStr = "${scopeClassNameStr}_ViewModelFactories_IntegrativeModule"
        val vmfimClassName = ClassName(integrationPkg, vmfimNameStr)

        return InterfaceSpec(vmfimClassName) {
            addContributesToAnnotation(vmScopeClassName)
            addAnnotation(ClassNames.bindingContainer)

            addFunction(FunSpec("bindWmMap") {
                addAnnotation(AnnotationSpec(ClassNames.multibinds) {
                    addMember("allowEmpty = true")
                })
                addAnnotation(ClassNames.sealantViewModelMap)
                addModifiers(KModifier.ABSTRACT)
                returns(ClassNames.viewModelMap)
            })

            addFunction(FunSpec("bindWmAssistedMap") {
                addAnnotation(AnnotationSpec(ClassNames.multibinds) {
                    addMember("allowEmpty = true")
                })
                addAnnotation(ClassNames.sealantViewModelAssistedMap)
                addModifiers(KModifier.ABSTRACT)
                returns(ClassNames.viewModelAssistedMap)
            })

            addOriginatingKSFile(fileNode)
        }
    }

    /**
     * Contributes to the `<Scope>_ViewModel`. This ensures the subcomponent officially exposes
     * the fully constructed `ViewModelFactories`, allowing the Android framework to finally
     * retrieve the instantiated ViewModels.
     * * * Output example:
     * ```kotlin
     * @ContributesTo(scope = <Scope>_ViewModel::class)
     * public interface <Scope>_ViewModelFactoriesOwner : ViewModelFactoriesOwner
     * ```
     */
    private fun buildVmFactoriesOwner(
        scopeClassNameStr: String,
        vmScopeClassName: ClassName,
        fileNode: KSFile,
    ): TypeSpec {
        val vmfoSnStr = ClassNames.viewModelFactoriesOwner.simpleName
        val vmfoClassName = ClassName(integrationPkg, "${scopeClassNameStr}_${vmfoSnStr}")

        return InterfaceSpec(vmfoClassName) {
            addSuperinterface(ClassNames.viewModelFactoriesOwner)
            addContributesToAnnotation(vmScopeClassName)

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
            return ViewModelIntegrationSymbolProcessor(
                codeGenerator = environment.codeGenerator,
                options = environment.options,
                logger = environment.logger,
            )
        }
    }
}
