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
package dev.steinerok.sealant.work.compiler.ksp

import com.google.auto.service.AutoService
import com.google.devtools.ksp.getConstructors
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.ksp.addOriginatingKSFile
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.writeTo
import dev.steinerok.sealant.compiler.AnnotationSpec
import dev.steinerok.sealant.compiler.ClassNames
import dev.steinerok.sealant.compiler.FunSpec
import dev.steinerok.sealant.compiler.InterfaceSpec
import dev.steinerok.sealant.compiler.ParameterSpec
import dev.steinerok.sealant.compiler.SealantFeature
import dev.steinerok.sealant.compiler.addContributesToAnnotation
import dev.steinerok.sealant.compiler.ksp.SealantFileSpec
import dev.steinerok.sealant.compiler.ksp.getSymbolsWithAnnotation
import dev.steinerok.sealant.compiler.ksp.hasSealantFeatureForScope
import dev.steinerok.sealant.compiler.ksp.implements
import dev.steinerok.sealant.compiler.ksp.isAnnotationPresent
import dev.steinerok.sealant.compiler.ksp.requireContainingFile
import dev.steinerok.sealant.compiler.ksp.scope

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
public class WorkerCreationSymbolProcessor(
    private val codeGenerator: CodeGenerator,
    @Suppress("unused") private val options: Map<String, String>,
    @Suppress("unused") private val logger: KSPLogger,
) : SymbolProcessor {

    override fun process(resolver: Resolver): List<KSAnnotated> {
        resolver
            .getSymbolsWithAnnotation(ClassNames.contributesWorker)
            .filterIsInstance<KSClassDeclaration>()
            .filter { annotated ->
                annotated
                    .scope()
                    .hasSealantFeatureForScope(SealantFeature.Work)
            }
            .onEach { clazz ->
                if (!clazz.implements(ClassNames.androidxListenableWorker)) {
                    logger.error(
                        message = "The annotation `@SealantWorker` can only be applied " +
                                "to classes which extend ${ClassNames.androidxListenableWorker}",
                        symbol = clazz
                    )
                }

                val constructor = clazz.getConstructors()
                    .singleOrNull { it.isAnnotationPresent(ClassNames.assistedInject) }
                if (clazz.getConstructors().toList().size != 1 || constructor == null) {
                    logger.error(
                        message = "Worker class, witch is annotated `@SealantWorker`, must have " +
                                "only one constructor and it must be annotated `@AssistedInject`",
                        symbol = clazz
                    )
                    return@onEach
                }

                val appContextParam = constructor.parameters.firstOrNull { param ->
                    param.isAnnotationPresent(ClassNames.assisted) &&
                            param.type.resolve().toClassName() == ClassNames.androidContext &&
                            param.name?.asString() == "appContext"
                }
                val paramsParam = constructor.parameters.firstOrNull { param ->
                    param.isAnnotationPresent(ClassNames.assisted) &&
                            param.type.resolve().toClassName() == ClassNames.workerParameters &&
                            param.name?.asString() == "workerParams"
                }
                val assistedParamsCount = constructor.parameters.count { param ->
                    param.isAnnotationPresent(ClassNames.assisted)
                }
                if (appContextParam == null || paramsParam == null || assistedParamsCount != 2) {
                    logger.error(
                        message = "Your constructor witch annotated `@AssistedInject` must have" +
                                "only 2 parameters annotated `@Assisted`: " +
                                "`appContext` with type ${ClassNames.androidContext} and " +
                                "`workerParams` with type ${ClassNames.workerParameters}",
                        symbol = clazz
                    )
                }
            }
            .forEach { symbol ->
                generateByProcessor(symbol).writeTo(
                    codeGenerator = codeGenerator,
                    aggregating = false,
                )
            }
        return emptyList()
    }

    private fun generateByProcessor(clazz: KSClassDeclaration): FileSpec {
        val packageName = clazz.packageName.asString()
        val fileName = clazz.simpleName.asString() + "_Creation"
        //
        val content = SealantFileSpec(packageName, fileName) {
            val origClassName = clazz.toClassName()
            val origShortName = clazz.simpleName.asString()
            val scopeClassName = clazz.scope().toClassName()
            //
            val afNameStr = "${origShortName}_AssistedFactory"
            val afClassName = ClassName(packageName, afNameStr)
            val afInterface = InterfaceSpec(afClassName) {
                addAnnotation(AnnotationSpec(ClassNames.assistedFactory))
                addSuperinterface(ClassNames.workerAssistedFactory.parameterizedBy(origClassName))
                addOriginatingKSFile(clazz.requireContainingFile())
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
                addOriginatingKSFile(clazz.requireContainingFile())
            }
            addType(bmInterface)
        }
        return content
    }

    /**
     * Entry point for KSP to pick up our [SymbolProcessor].
     */
    @Suppress("unused")
    @AutoService(SymbolProcessorProvider::class)
    public class Provider : SymbolProcessorProvider {

        override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
            return WorkerCreationSymbolProcessor(
                codeGenerator = environment.codeGenerator,
                options = environment.options,
                logger = environment.logger,
            )
        }
    }
}
