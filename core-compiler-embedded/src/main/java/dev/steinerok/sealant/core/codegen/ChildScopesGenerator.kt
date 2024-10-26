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
package dev.steinerok.sealant.core.codegen

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
import com.squareup.kotlinpoet.KModifier
import dev.steinerok.sealant.compiler.ClassSpec
import dev.steinerok.sealant.compiler.ConstructorSpec
import dev.steinerok.sealant.compiler.SealantFeature
import dev.steinerok.sealant.compiler.buildVmScopeClassName
import dev.steinerok.sealant.compiler.embedded.AlwaysApplicableCodeGenerator
import dev.steinerok.sealant.compiler.embedded.FqNames
import dev.steinerok.sealant.compiler.embedded.SealantFileSpecContent
import dev.steinerok.sealant.compiler.embedded.hasSealantFeatureForScope
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.psi.KtFile
import java.io.File

/**
 * Should generate:
 * ```
 * public abstract class ViewModel_<Scope> private constructor()
 * ```
 */
@AutoService(CodeGenerator::class)
public class ChildScopesGenerator : AlwaysApplicableCodeGenerator {

    override fun generateCode(
        codeGenDir: File,
        module: ModuleDescriptor,
        projectFiles: Collection<KtFile>
    ): Collection<FileWithContent> = projectFiles
        .classAndInnerClassReferences(module)
        .filter { clazz ->
            clazz.isAnnotatedWith(FqNames.sealantConfiguration)
        }
        .onEach {
            // TODO: Verification if need
        }
        .flatMap { clazz ->
            listOfNotNull(generateScopes(codeGenDir, clazz))
        }
        .toList()

    private fun generateScopes(codeGenDir: File, clazz: ClassReference): GeneratedFileWithSources? {
        val hasViewModelSupport = clazz.hasSealantFeatureForScope(SealantFeature.ViewModel)
        if (!hasViewModelSupport) return null
        //
        val packageName = clazz.packageFqName.safePackageString(dotSuffix = false)
        val fileName =
            clazz.generateClassName().relativeClassName.asString() + "_SealantChildScopes"
        val origClassName = clazz.asClassName()
        //
        val content = SealantFileSpecContent(packageName, fileName) {
            //
            val vmScopeClassName = buildVmScopeClassName(origClassName)
            val vmScopeClass = ClassSpec(vmScopeClassName) {
                addModifiers(KModifier.ABSTRACT)
                primaryConstructor(
                    ConstructorSpec {
                        addModifiers(KModifier.PRIVATE)
                    }
                )
            }
            addType(vmScopeClass)
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
