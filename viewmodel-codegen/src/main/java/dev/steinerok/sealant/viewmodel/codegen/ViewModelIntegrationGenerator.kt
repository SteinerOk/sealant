/*
 * Copyright 2022 Ihor Kushnirenko
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
package dev.steinerok.sealant.viewmodel.codegen

import com.google.auto.service.AutoService
import com.squareup.anvil.compiler.api.CodeGenerator
import com.squareup.anvil.compiler.api.FileWithContent
import com.squareup.anvil.compiler.api.GeneratedFileWithSources
import com.squareup.anvil.compiler.api.createGeneratedFile
import com.squareup.anvil.compiler.internal.reference.ClassReference
import com.squareup.anvil.compiler.internal.reference.asClassName
import com.squareup.anvil.compiler.internal.reference.classAndInnerClassReferences
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.KModifier
import dev.steinerok.sealant.compiler.AnnotationSpec
import dev.steinerok.sealant.compiler.ClassNames
import dev.steinerok.sealant.compiler.FunSpec
import dev.steinerok.sealant.compiler.InterfaceSpec
import dev.steinerok.sealant.compiler.ParameterSpec
import dev.steinerok.sealant.compiler.SealantFeature
import dev.steinerok.sealant.compiler.addContributesToAnnotation
import dev.steinerok.sealant.compiler.buildVmScopeClassName
import dev.steinerok.sealant.compiler.embedded.AlwaysApplicableCodeGenerator
import dev.steinerok.sealant.compiler.embedded.SealantFileSpecContent
import dev.steinerok.sealant.compiler.embedded.getScopeFromComponentOrSubcomponent
import dev.steinerok.sealant.compiler.embedded.isComponentOrSubcomponentWithSealantFeature
import dev.steinerok.sealant.compiler.generateSimpleNameString
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.psi.KtFile
import java.io.File

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
@AutoService(CodeGenerator::class)
public class ViewModelIntegrationGenerator : AlwaysApplicableCodeGenerator {

    override fun generateCode(
        codeGenDir: File,
        module: ModuleDescriptor,
        projectFiles: Collection<KtFile>
    ): Collection<FileWithContent> = projectFiles
        .classAndInnerClassReferences(module)
        .filter { clazz ->
            clazz.isComponentOrSubcomponentWithSealantFeature(SealantFeature.ViewModel)
        }
        // fast path for excluding duplicate binding modules if they're in the same source
        .distinctBy { clazz -> clazz.getScopeFromComponentOrSubcomponent() }
        .onEach { _ -> /* Verification if you need */ }
        .flatMap { clazz ->
            listOfNotNull(generateIntegration(codeGenDir, clazz))
        }
        .toList()

    private fun generateIntegration(
        codeGenDir: File,
        clazz: ClassReference,
    ): GeneratedFileWithSources {
        val packageName = integrationPkg
        val scopeFqName = clazz.getScopeFromComponentOrSubcomponent()
        val scopeClassName = scopeFqName.asClassName()
        val scopeClassNameStr = scopeClassName.generateSimpleNameString()
        val fileName = "${scopeClassNameStr}_${featureName}_Integration"
        //
        val content = SealantFileSpecContent(packageName, fileName) {
            val vmScopeClassName = buildVmScopeClassName(scopeClassName)
            //
            val vmsNameStr =
                "${scopeClassNameStr}_${ClassNames.sealantViewModelSubcomponent.simpleName}"
            val vmsClassName = ClassName(packageName, vmsNameStr)
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
                    }
                )
                addType(
                    InterfaceSpec("Parent") {
                        addSuperinterface(ClassNames.sealantViewModelSubcomponentParent)
                        addContributesToAnnotation(scopeClassName)
                    }
                )
            }
            addType(vmsInterface)
            //
            val imNameStr =
                "${scopeClassNameStr}_${ClassNames.sealantViewModelSubcomponent.simpleName}_IntegrativeModule"
            val imClassName = ClassName(packageName, imNameStr)
            val imInterface = InterfaceSpec(imClassName) {
                addAnnotation(
                    AnnotationSpec(ClassNames.module) {
                        addMember("subcomponents·=·[%T::class]", vmsClassName)
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
                                vmsClassName.nestedClass("Factory")
                            )
                        )
                        returns(ClassNames.sealantViewModelSubcomponentFactory)
                    }
                )
            }
            addType(imInterface)
            //
            val wmfcoNameStr =
                "${scopeClassNameStr}_${ClassNames.sealantViewModelFactoryCreatorOwner.generateSimpleNameString()}"
            val wmfcoClassName = ClassName(packageName, wmfcoNameStr)
            val wmfcoInterface = InterfaceSpec(wmfcoClassName) {
                addSuperinterface(ClassNames.sealantViewModelFactoryCreatorOwner)
                addContributesToAnnotation(scopeClassName)
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
            }
            addType(vmfimInterface)
            //
            val vmfoNameStr =
                "${scopeClassNameStr}_${ClassNames.viewModelFactoriesOwner.simpleName}"
            val vmfoClassName = ClassName(packageName, vmfoNameStr)
            val vmfoInterface = InterfaceSpec(vmfoClassName) {
                addSuperinterface(ClassNames.viewModelFactoriesOwner)
                addContributesToAnnotation(vmScopeClassName)
            }
            addType(vmfoInterface)
        }
        return createGeneratedFile(
            codeGenDir = codeGenDir,
            packageName = packageName,
            fileName = fileName,
            content = content,
            sourceFile = clazz.containingFileAsJavaFile,
        )
    }
}
