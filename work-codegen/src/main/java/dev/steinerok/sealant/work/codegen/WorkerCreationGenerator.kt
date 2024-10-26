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
package dev.steinerok.sealant.work.codegen

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
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import dev.steinerok.sealant.compiler.AnnotationSpec
import dev.steinerok.sealant.compiler.ClassNames
import dev.steinerok.sealant.compiler.FunSpec
import dev.steinerok.sealant.compiler.InterfaceSpec
import dev.steinerok.sealant.compiler.ParameterSpec
import dev.steinerok.sealant.compiler.SealantFeature
import dev.steinerok.sealant.compiler.addContributesToAnnotation
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
 * @AssistedFactory
 * public interface <Worker>_AssistedFactory : WorkerAssistedFactory<<Worker>>
 *
 * @Module
 * @ContributesTo(scope = <Scope>::class)
 * public interface <Worker>_BindsModule {
 *     @Binds
 *     @IntoMap
 *     @StringKey("pkg.<Worker>")
 *     @SealantWorkerAssistedFactoryMap
 *     public fun bind(instance: <Worker>_AssistedFactory): WorkerAssistedFactory<out ListenableWorker>
 * }
 * ```
 */
@AutoService(CodeGenerator::class)
public class WorkerCreationGenerator : AlwaysApplicableCodeGenerator {

    override fun generateCode(
        codeGenDir: File,
        module: ModuleDescriptor,
        projectFiles: Collection<KtFile>
    ): Collection<FileWithContent> = projectFiles
        .classAndInnerClassReferences(module)
        .filter { clazz ->
            clazz.isAnnotatedWith(FqNames.contributesWorker) &&
                    clazz.getScopeFrom(FqNames.contributesWorker)
                        .hasSealantFeatureForScope(SealantFeature.Work)
        }
        .onEach { clazz ->
            if (!clazz.implements(ClassNames.androidxListenableWorker)) {
                throw AnvilCompilationExceptionClassReference(
                    message = "The annotation `@SealantWorker` can only be applied " +
                            "to classes which extend ${ClassNames.androidxListenableWorker}",
                    classReference = clazz
                )
            }
            val constructor = clazz.constructors
                .singleOrNull { it.isAnnotatedWith(FqNames.assistedInject) }
            if (clazz.constructors.size != 1 || constructor == null) {
                throw AnvilCompilationExceptionClassReference(
                    message = "Worker class, witch is annotated `@SealantWorker`, must have " +
                            "only one constructor and it must be annotated `@AssistedInject`",
                    classReference = clazz
                )
            }
            val appContextParam = constructor.parameters.firstOrNull { param ->
                param.type().asClassReference().asClassName() == ClassNames.androidContext &&
                        param.isAnnotatedWith(FqNames.assisted) &&
                        param.name == "appContext"
            }
            val paramsParam = constructor.parameters.firstOrNull { param ->
                param.type().asClassReference().asClassName() == ClassNames.workerParameters &&
                        param.isAnnotatedWith(FqNames.assisted) &&
                        param.name == "workerParams"
            }
            val assistedParamsCount = constructor.parameters.count { param ->
                param.isAnnotatedWith(FqNames.assisted)
            }
            if (appContextParam == null || paramsParam == null || assistedParamsCount != 2) {
                throw AnvilCompilationExceptionClassReference(
                    message = "Your constructor witch annotated `@AssistedInject` must have" +
                            "only 2 parameters annotated `@Assisted`: " +
                            "`appContext` with type ${ClassNames.androidContext} and " +
                            "`workerParams` with type ${ClassNames.workerParameters}",
                    classReference = clazz
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
            val scopeFqName = clazz.getScopeFrom(FqNames.contributesWorker)
            val scopeClassName = scopeFqName.asClassName()
            //
            val afNameStr = "${origShortName}_AssistedFactory"
            val afClassName = ClassName(packageName, afNameStr)
            val afInterface = InterfaceSpec(afClassName) {
                addAnnotation(AnnotationSpec(ClassNames.assistedFactory))
                addSuperinterface(ClassNames.workerAssistedFactory.parameterizedBy(origClassName))
            }
            addType(afInterface)
            //
            val bmNameStr = "${origShortName}_BindsModule"
            val bmClassName = ClassName(packageName, bmNameStr)
            val bmInterface = InterfaceSpec(bmClassName) {
                addAnnotation(ClassNames.module)
                addContributesToAnnotation(scopeClassName)
                addFunction(
                    FunSpec("bind") {
                        addAnnotation(ClassNames.binds)
                        addAnnotation(ClassNames.intoMap)
                        addAnnotation(
                            AnnotationSpec(ClassNames.stringKey) {
                                addMember("%S", origClassName.reflectionName().replace("..", "."))
                            }
                        )
                        addAnnotation(ClassNames.sealantWorkerAssistedFactoryMap)
                        addModifiers(KModifier.ABSTRACT)
                        addParameter(ParameterSpec("instance", afClassName))
                        returns(ClassNames.workerAssistedFactoryOutListenableWorker)
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
