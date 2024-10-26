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
package dev.steinerok.sealant.appcomponent.codegen

import com.google.auto.service.AutoService
import com.squareup.anvil.compiler.api.CodeGenerator
import com.squareup.anvil.compiler.api.FileWithContent
import com.squareup.anvil.compiler.api.GeneratedFileWithSources
import com.squareup.anvil.compiler.api.createGeneratedFile
import com.squareup.anvil.compiler.internal.reference.ClassReference
import com.squareup.anvil.compiler.internal.reference.asClassName
import com.squareup.anvil.compiler.internal.reference.classAndInnerClassReferences
import com.squareup.anvil.compiler.internal.reference.generateClassName
import com.squareup.anvil.compiler.internal.safePackageString
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.STAR
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
import dev.steinerok.sealant.compiler.embedded.AlwaysApplicableCodeGenerator
import dev.steinerok.sealant.compiler.embedded.FqNames
import dev.steinerok.sealant.compiler.embedded.SealantFileSpecContent
import dev.steinerok.sealant.compiler.embedded.getScopeFrom
import dev.steinerok.sealant.compiler.embedded.hasSealantFeatureForScope
import dev.steinerok.sealant.compiler.embedded.implements
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.psi.KtFile
import java.io.File

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
@AutoService(CodeGenerator::class)
public class AppcomponentInjectionGenerator : AlwaysApplicableCodeGenerator {

    override fun generateCode(
        codeGenDir: File,
        module: ModuleDescriptor,
        projectFiles: Collection<KtFile>
    ): Collection<FileWithContent> = projectFiles
        .classAndInnerClassReferences(module)
        .filter { clazz ->
            clazz.isAnnotatedWith(FqNames.injectWith) &&
                    clazz.getScopeFrom(FqNames.injectWith)
                        .hasSealantFeatureForScope(SealantFeature.Appcomponent)
        }
        .onEach { _ -> /* Verification if you need */ }
        .flatMap { clazz ->
            listOfNotNull(generateComponent(codeGenDir, clazz))
        }
        .toList()

    private fun generateComponent(
        codeGenDir: File,
        clazz: ClassReference,
    ): GeneratedFileWithSources {
        val packageName = clazz.packageFqName.safePackageString(dotSuffix = false)
        val fileName = clazz.generateClassName().relativeClassName.asString() + "_Injection"
        //
        val content = SealantFileSpecContent(packageName, fileName) {
            val origClassName = clazz.asClassName()
            val origShortName = clazz.shortName
            val scopeFqName = clazz.getScopeFrom(FqNames.injectWith)
            val scopeClassName = scopeFqName.asClassName()
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
            }
            addType(ibmInterface)
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
