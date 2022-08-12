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
package dev.steinerok.sealant.work.generator

import com.google.auto.service.AutoService
import com.squareup.anvil.compiler.api.CodeGenerator
import com.squareup.anvil.compiler.api.GeneratedFile
import com.squareup.anvil.compiler.api.createGeneratedFile
import com.squareup.anvil.compiler.internal.reference.ClassReference
import com.squareup.anvil.compiler.internal.reference.asClassName
import com.squareup.anvil.compiler.internal.reference.classAndInnerClassReferences
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.KModifier
import dev.steinerok.sealant.core.ClassNames
import dev.steinerok.sealant.core.ClassSpec
import dev.steinerok.sealant.core.CompanionObjectSpec
import dev.steinerok.sealant.core.FunSpec
import dev.steinerok.sealant.core.InterfaceSpec
import dev.steinerok.sealant.core.ParameterSpec
import dev.steinerok.sealant.core.SealantFeature
import dev.steinerok.sealant.core.addContributesTo
import dev.steinerok.sealant.core.buildFile
import dev.steinerok.sealant.core.generateSimpleNameString
import dev.steinerok.sealant.core.generator.AlwaysApplicableCodeGenerator
import dev.steinerok.sealant.core.getScopeFromComponentOrSubcomponent
import dev.steinerok.sealant.core.hasSealantFeatureInParents
import dev.steinerok.sealant.core.isComponentOrSubcomponentWithSealantFeature
import dev.steinerok.sealant.work.featureName
import dev.steinerok.sealant.work.integrationPkg
import dev.steinerok.sealant.work.sealantWorkerAssistedFactoryMap
import dev.steinerok.sealant.work.sealantWorkerFactory
import dev.steinerok.sealant.work.sealantWorkerFactoryOwner
import dev.steinerok.sealant.work.workerAssistedFactoryMap
import dev.steinerok.sealant.work.workerAssistedFactoryProviderMap
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.psi.KtFile
import java.io.File

/**
 * Should generate:
 * ```
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
 *
 * @ContributesTo(scope = <Scope>::class)
 * public interface <Scope>_SealantWorkerFactory_Owner : SealantWorkerFactory.Owner
 * ```
 */
@AutoService(CodeGenerator::class)
public class WorkIntegrationGenerator : AlwaysApplicableCodeGenerator {

    override fun generateCode(
        codeGenDir: File,
        module: ModuleDescriptor,
        projectFiles: Collection<KtFile>
    ): Collection<GeneratedFile> = projectFiles
        .classAndInnerClassReferences(module)
        .filter { clazz ->
            clazz.isComponentOrSubcomponentWithSealantFeature(SealantFeature.Work)
        }
        // fast path for excluding duplicate binding modules if they're in the same source
        .distinctBy { clazz -> clazz.getScopeFromComponentOrSubcomponent() }
        .onEach {
            // TODO: Verification if need
        }
        .flatMap { clazz ->
            listOfNotNull(generateIntegration(codeGenDir, clazz))
        }
        .toList()

    private fun generateIntegration(codeGenDir: File, clazz: ClassReference): GeneratedFile {
        val packageName = integrationPkg
        val scopeFqName = clazz.getScopeFromComponentOrSubcomponent()
        val scopeClassName = scopeFqName.asClassName()
        val scopeClassNameStr = scopeClassName.generateSimpleNameString()
        val fileName = "${scopeClassNameStr}_${featureName}_Integration"
        //
        val content = FileSpec.buildFile(packageName, fileName) {
            val hasFeatureInParents = clazz.hasSealantFeatureInParents(SealantFeature.Work)
            if (!hasFeatureInParents) {
                //
                val wimNameStr = "${scopeClassNameStr}_${featureName}_IntegrativeModule"
                val wimClassName = ClassName(packageName, wimNameStr)
                val wimClass = ClassSpec(wimClassName) {
                    addModifiers(KModifier.ABSTRACT)
                    addAnnotation(ClassNames.module)
                    addContributesTo(scopeClassName)
                    addFunction(
                        FunSpec(name = "bindWafMap") {
                            addAnnotation(ClassNames.multibinds)
                            addAnnotation(ClassNames.sealantWorkerAssistedFactoryMap)
                            addModifiers(KModifier.ABSTRACT)
                            returns(ClassNames.workerAssistedFactoryMap)
                        }
                    )
                    val companion = CompanionObjectSpec {
                        addFunction(
                            FunSpec(name = "provideWorkerFactory") {
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
                                    "return·%T(wafProviderMap)",
                                    ClassNames.sealantWorkerFactory
                                )
                            }
                        )
                    }
                    addType(companion)
                }
                addType(wimClass)
            }
            //
            val wfoNameStr =
                "${scopeClassNameStr}_${ClassNames.sealantWorkerFactoryOwner.generateSimpleNameString()}"
            val wfoClassName = ClassName(packageName, wfoNameStr)
            val wfoInterface = InterfaceSpec(wfoClassName) {
                addSuperinterface(ClassNames.sealantWorkerFactoryOwner)
                addContributesTo(scopeClassName)
            }
            addType(wfoInterface)
        }
        return createGeneratedFile(codeGenDir, packageName, fileName, content)
    }
}
