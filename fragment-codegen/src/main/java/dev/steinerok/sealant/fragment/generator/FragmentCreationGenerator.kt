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
import com.squareup.anvil.compiler.api.FileWithContent
import com.squareup.anvil.compiler.api.GeneratedFileWithSources
import com.squareup.anvil.compiler.api.createGeneratedFile
import com.squareup.anvil.compiler.internal.reference.ClassReference
import com.squareup.anvil.compiler.internal.reference.asClassName
import com.squareup.anvil.compiler.internal.reference.classAndInnerClassReferences
import com.squareup.anvil.compiler.internal.reference.generateClassName
import com.squareup.anvil.compiler.internal.safePackageString
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.KModifier
import dev.steinerok.sealant.core.AnnotationSpec
import dev.steinerok.sealant.core.ClassNames
import dev.steinerok.sealant.core.FqNames
import dev.steinerok.sealant.core.FunSpec
import dev.steinerok.sealant.core.InterfaceSpec
import dev.steinerok.sealant.core.ParameterSpec
import dev.steinerok.sealant.core.SealantFeature
import dev.steinerok.sealant.core.addContributesTo
import dev.steinerok.sealant.core.buildFile
import dev.steinerok.sealant.core.generator.AlwaysApplicableCodeGenerator
import dev.steinerok.sealant.core.getScopeFrom
import dev.steinerok.sealant.core.hasSealantFeatureForScope
import dev.steinerok.sealant.core.isFragment
import dev.steinerok.sealant.fragment.contributesFragment
import dev.steinerok.sealant.fragment.fragmentKey
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.psi.KtFile
import java.io.File

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
@AutoService(CodeGenerator::class)
public class FragmentCreationGenerator : AlwaysApplicableCodeGenerator {

    override fun generateCode(
        codeGenDir: File,
        module: ModuleDescriptor,
        projectFiles: Collection<KtFile>
    ): Collection<FileWithContent> = projectFiles
        .classAndInnerClassReferences(module)
        .filter { clazz ->
            clazz.isAnnotatedWith(FqNames.contributesFragment) &&
                    clazz.getScopeFrom(FqNames.contributesFragment)
                        .hasSealantFeatureForScope(SealantFeature.Fragment)
        }
        .onEach { clazz ->
            require(clazz.isFragment()) {
                "The annotation `@ContributesFragment` can only be applied " +
                        "to classes which extend ${FqNames.androidxFragment.asString()}"
            }
        }
        .flatMap { clazz ->
            listOfNotNull(generateCreation(codeGenDir, clazz))
        }
        .toList()

    private fun generateCreation(
        codeGenDir: File,
        clazz: ClassReference,
    ): GeneratedFileWithSources {
        val packageName = clazz.packageFqName.safePackageString(dotSuffix = false)
        val fileName = clazz.generateClassName().relativeClassName.asString() + "_Creation"
        //
        val content = FileSpec.buildFile(packageName, fileName) {
            val origClassName = clazz.asClassName()
            val origShortName = clazz.shortName
            val scopeFqName = clazz.getScopeFrom(FqNames.contributesFragment)
            val scopeClassName = scopeFqName.asClassName()
            //
            val bmNameStr = "${origShortName}_BindsModule"
            val bmClassName = ClassName(packageName, bmNameStr)
            val bmInterface = InterfaceSpec(bmClassName) {
                addAnnotation(ClassNames.module)
                addContributesTo(scopeClassName) {
                    val replaces = clazz.annotations
                        .first { it.fqName == FqNames.contributesFragment }
                        .replaces(parameterIndex = 1)
                        .map { it.asClassName() }
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
            }
            addType(bmInterface)
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
