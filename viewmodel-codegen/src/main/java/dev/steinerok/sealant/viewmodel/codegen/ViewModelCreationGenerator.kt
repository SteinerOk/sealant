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
import com.squareup.anvil.compiler.internal.reference.AnvilCompilationExceptionClassReference
import com.squareup.anvil.compiler.internal.reference.ClassReference
import com.squareup.anvil.compiler.internal.reference.asClassName
import com.squareup.anvil.compiler.internal.reference.classAndInnerClassReferences
import com.squareup.anvil.compiler.internal.reference.generateClassName
import com.squareup.anvil.compiler.internal.safePackageString
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.KModifier
import dev.steinerok.sealant.compiler.AnnotationSpec
import dev.steinerok.sealant.compiler.ClassNames
import dev.steinerok.sealant.compiler.FunSpec
import dev.steinerok.sealant.compiler.InterfaceSpec
import dev.steinerok.sealant.compiler.ObjectSpec
import dev.steinerok.sealant.compiler.ParameterSpec
import dev.steinerok.sealant.compiler.SealantFeature
import dev.steinerok.sealant.compiler.addContributesToAnnotation
import dev.steinerok.sealant.compiler.buildVmScopeClassName
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
 * Source generator to support Sealant injection of ViewModels.
 *
 * Should generate:
 * ```
 * @Module
 * @ContributesTo(scope = <Scope>::class)
 * public object <ViewModel>_KeyModule {
 *   @Provides
 *   @IntoSet
 *   @SealantViewModelMap.KeySet
 *   public fun provide<ViewModel>Key(): Class<out ViewModel> = <ViewModel>::class.java
 * }
 *
 * @Module
 * @ContributesTo(scope = ViewModel_<Scope>::class)
 * public interface <ViewModel>_BindsModule {
 *   @Binds
 *   @IntoMap
 *   @ViewModelKey(<ViewModel>::class)
 *   @SealantViewModelMap
 *   public fun bind(instance: <ViewModel>): ViewModel
 * }
 * ```
 *
 * related with Hilt codegen:
 * ```
 * public final class $_HiltModules {
 *   @Module
 *   @InstallIn(ViewModelComponent.class)
 *   public static abstract class BindsModule {
 *     @Binds
 *     @IntoMap
 *     @StringKey("pkg.$")
 *     @HiltViewModelMap
 *     public abstract ViewModel bind($ vm)
 *   }
 *
 *   @Module
 *   @InstallIn(ActivityRetainedComponent.class)
 *   public static final class KeyModule {
 *     @Provides
 *     @IntoSet
 *     @HiltViewModelMap.KeySet
 *     public static String provide() {
 *      return "pkg.$";
 *     }
 *   }
 * }
 * ```
 */
@AutoService(CodeGenerator::class)
public class ViewModelCreationGenerator : AlwaysApplicableCodeGenerator {

    override fun generateCode(
        codeGenDir: File,
        module: ModuleDescriptor,
        projectFiles: Collection<KtFile>
    ): Collection<FileWithContent> = projectFiles
        .classAndInnerClassReferences(module)
        .filter { clazz ->
            clazz.isAnnotatedWith(FqNames.contributesViewModel) &&
                    clazz.getScopeFrom(FqNames.contributesViewModel)
                        .hasSealantFeatureForScope(SealantFeature.ViewModel)
        }
        .onEach { clazz ->
            if (!clazz.implements(ClassNames.androidxViewModel)) {
                throw AnvilCompilationExceptionClassReference(
                    message = "The annotation `@SealantViewModel` can only be applied " +
                            "to classes which extend ${ClassNames.androidxViewModel}",
                    classReference = clazz,
                )
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
        val content = SealantFileSpecContent(packageName, fileName) {
            val origClassName = clazz.asClassName()
            val origShortName = clazz.shortName
            val scopeFqName = clazz.getScopeFrom(FqNames.contributesViewModel)
            val scopeClassName = scopeFqName.asClassName()
            val vmScopeClassName = buildVmScopeClassName(scopeClassName)
            //
            val kmNameStr = "${origShortName}_KeyModule"
            val kmClassName = ClassName(packageName, kmNameStr)
            val kmObject = ObjectSpec(kmClassName) {
                addAnnotation(ClassNames.module)
                addContributesToAnnotation(scopeClassName)
                addFunction(
                    FunSpec("provide${origShortName}Key") {
                        addAnnotation(ClassNames.provides)
                        addAnnotation(ClassNames.intoSet)
                        addAnnotation(ClassNames.sealantViewModelMapKeySet)
                        returns(ClassNames.javaClazzOutViewModel)
                        addStatement("return·%T::class.java", origClassName)
                    }
                )
            }
            addType(kmObject)
            //
            val bmNameStr = "${origShortName}_BindsModule"
            val bmClassName = ClassName(packageName, bmNameStr)
            val bmInterface = InterfaceSpec(bmClassName) {
                addAnnotation(ClassNames.module)
                addContributesToAnnotation(vmScopeClassName)
                addFunction(
                    FunSpec("bind") {
                        addAnnotation(ClassNames.binds)
                        addAnnotation(ClassNames.intoMap)
                        addAnnotation(
                            AnnotationSpec(ClassNames.viewModelKey) {
                                addMember("%T::class", origClassName)
                            }
                        )
                        addAnnotation(ClassNames.sealantViewModelMap)
                        addModifiers(KModifier.ABSTRACT)
                        addParameter(ParameterSpec("instance", origClassName))
                        returns(ClassNames.androidxViewModel)
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
