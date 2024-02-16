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
package dev.steinerok.sealant.viewmodel.generator

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
import dev.steinerok.sealant.core.AnnotationSpec
import dev.steinerok.sealant.core.ClassNames
import dev.steinerok.sealant.core.FqNames
import dev.steinerok.sealant.core.InterfaceSpec
import dev.steinerok.sealant.core.SealantFeature
import dev.steinerok.sealant.core.addContributesTo
import dev.steinerok.sealant.core.buildFile
import dev.steinerok.sealant.core.buildVmScopeClassName
import dev.steinerok.sealant.core.generator.AlwaysApplicableCodeGenerator
import dev.steinerok.sealant.core.getScopeFrom
import dev.steinerok.sealant.core.hasSealantFeatureForScope
import dev.steinerok.sealant.viewmodel.contributesToViewModel
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.psi.KtFile
import java.io.File

/**
 * Should generate:
 * ```
 * @Module(includes = [<Module>::class])
 * @ContributesTo(scope = ViewModel_<Scope>::class)
 * public interface <Module>_Wrapper
 * ```
 */
@AutoService(CodeGenerator::class)
public class ViewModelSubcomponentModuleWrapperGenerator : AlwaysApplicableCodeGenerator {

    override fun generateCode(
        codeGenDir: File,
        module: ModuleDescriptor,
        projectFiles: Collection<KtFile>
    ): Collection<FileWithContent> = projectFiles
        .classAndInnerClassReferences(module)
        .filter { clazz ->
            clazz.isAnnotatedWith(FqNames.contributesToViewModel) &&
                    clazz.getScopeFrom(FqNames.contributesToViewModel)
                        .hasSealantFeatureForScope(SealantFeature.ViewModel)
        }
        .onEach {
            // TODO: Verification if need
        }
        .flatMap { clazz ->
            listOfNotNull(generateWrapper(codeGenDir, clazz))
        }
        .toList()

    private fun generateWrapper(codeGenDir: File, clazz: ClassReference): GeneratedFileWithSources {
        val packageName = clazz.packageFqName.safePackageString(dotSuffix = false)
        val fileName = clazz.generateClassName().relativeClassName.asString() + "_Wrapper"
        //
        val content = FileSpec.buildFile(packageName, fileName) {
            val origClassName = clazz.asClassName()
            val origShortName = clazz.shortName
            val scopeFqName = clazz.getScopeFrom(FqNames.contributesToViewModel)
            val scopeClassName = scopeFqName.asClassName()
            val vmScopeClassName = buildVmScopeClassName(scopeClassName)
            //
            val wNameStr = "${origShortName}_Wrapper"
            val wClassName = ClassName(packageName, wNameStr)
            val wInterface = InterfaceSpec(wClassName) {
                addAnnotation(
                    AnnotationSpec(ClassNames.module) {
                        addMember("includes·=·[%T::class]", origClassName)
                    }
                )
                addContributesTo(vmScopeClassName)
            }
            addType(wInterface)
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
