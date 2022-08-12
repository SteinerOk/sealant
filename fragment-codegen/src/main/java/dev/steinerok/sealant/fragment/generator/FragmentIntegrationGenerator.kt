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
package dev.steinerok.sealant.fragment.generator

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
import dev.steinerok.sealant.fragment.featureName
import dev.steinerok.sealant.fragment.fragmentMap
import dev.steinerok.sealant.fragment.fragmentProviderMap
import dev.steinerok.sealant.fragment.integrationPkg
import dev.steinerok.sealant.fragment.sealantFragmentFactory
import dev.steinerok.sealant.fragment.sealantFragmentFactoryOwner
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.psi.KtFile
import java.io.File

/**
 * Should generate:
 * ```
 * @Module
 * @ContributesTo(scope = <Scope>::class)
 * public abstract class <Scope>_sealantFragment_IntegrativeModule {
 *
 *     @Multibinds
 *     public abstract fun bindFragmentMap(): Map<Class<out Fragment>, Fragment>
 *
 *     public companion object {
 *         @Provides
 *         public fun provideFragmentFactory(
 *             fragmentProviderMap: Map<Class<out Fragment>, @JvmSuppressWildcards Provider<Fragment>>
 *         ): sealantFragmentFactory = sealantFragmentFactory(fragmentProviderMap)
 *     }
 * }
 *
 * @ContributesTo(scope = <Scope>::class)
 * public interface <Scope>_SealantFragmentFactoryOwner : SealantFragmentFactory.Owner
 * ```
 */
@AutoService(CodeGenerator::class)
public class FragmentIntegrationGenerator : AlwaysApplicableCodeGenerator {

    override fun generateCode(
        codeGenDir: File,
        module: ModuleDescriptor,
        projectFiles: Collection<KtFile>
    ): Collection<GeneratedFile> = projectFiles
        .classAndInnerClassReferences(module)
        .filter { clazz ->
            clazz.isComponentOrSubcomponentWithSealantFeature(SealantFeature.Fragment)
        }
        // fast path for excluding duplicate binding modules if they're in the same source
        .distinctBy { clazz -> clazz.getScopeFromComponentOrSubcomponent() }
        .onEach {
            // TODO: Verification
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
            val hasFeatureInParents = clazz.hasSealantFeatureInParents(SealantFeature.Fragment)
            if (!hasFeatureInParents) {
                //
                val imNameStr = "${scopeClassNameStr}_${featureName}_IntegrativeModule"
                val imClassName = ClassName(packageName, imNameStr)
                val imClass = ClassSpec(imClassName) {
                    addModifiers(KModifier.ABSTRACT)
                    addAnnotation(ClassNames.module)
                    addContributesTo(scopeClassName)
                    addFunction(
                        FunSpec("bindFragmentMap") {
                            addAnnotation(ClassNames.multibinds)
                            addModifiers(KModifier.ABSTRACT)
                            returns(ClassNames.fragmentMap)
                        }
                    )
                    val companion = CompanionObjectSpec {
                        addFunction(
                            FunSpec("provideFragmentFactory") {
                                addAnnotation(ClassNames.provides)
                                addParameter(
                                    ParameterSpec(
                                        "fragmentProviderMap",
                                        ClassNames.fragmentProviderMap
                                    )
                                )
                                returns(ClassNames.sealantFragmentFactory)
                                addStatement(
                                    "return·%T(fragmentProviderMap)",
                                    ClassNames.sealantFragmentFactory
                                )
                            }
                        )
                    }
                    addType(companion)
                }
                addType(imClass)
            }
            //
            val ffoNameStr =
                "${scopeClassNameStr}_${ClassNames.sealantFragmentFactoryOwner.generateSimpleNameString()}"
            val ffoClassName = ClassName(packageName, ffoNameStr)
            val ffoInterface = InterfaceSpec(ffoClassName) {
                addSuperinterface(ClassNames.sealantFragmentFactoryOwner)
                addContributesTo(scopeClassName)
            }
            addType(ffoInterface)
        }
        return createGeneratedFile(codeGenDir, packageName, fileName, content)
    }
}
