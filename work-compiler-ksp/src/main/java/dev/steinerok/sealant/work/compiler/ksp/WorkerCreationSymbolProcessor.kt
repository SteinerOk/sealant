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
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.validate
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeSpec
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
import dev.steinerok.sealant.compiler.ksp.simpleValidatePredicate

/**
 * Generates assisted-worker bindings for classes annotated with `@ContributesWorker`.
 *
 * Sealant uses these bindings to connect WorkManager runtime parameters with Metro-provided
 * dependencies through a custom [dev.steinerok.sealant.work.SealantWorkerFactory].
 */
public class WorkerCreationSymbolProcessor(
    private val codeGenerator: CodeGenerator,
    @Suppress("unused") private val options: Map<String, String>,
    @Suppress("unused") private val logger: KSPLogger,
) : SymbolProcessor {

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val (validSymbols, invalidSymbols) = resolver
            .getSymbolsWithAnnotation(ClassNames.contributesWorker)
            .filterIsInstance<KSClassDeclaration>()
            .partition { symbol -> symbol.validate(simpleValidatePredicate) }

        validSymbols
            .filter { annotated ->
                annotated
                    .scope()
                    .hasSealantFeatureForScope(SealantFeature.Work)
            }
            .forEach { symbol ->
                generateByProcessor(symbol)?.writeTo(
                    codeGenerator = codeGenerator,
                    aggregating = false, // Isolating mode
                )
            }

        return invalidSymbols
    }

    private fun generateByProcessor(clazz: KSClassDeclaration): FileSpec? {
        if (!clazz.implements(ClassNames.androidxListenableWorker)) {
            logger.error(
                message = "The annotation `@SealantWorker` can only be applied " +
                        "to classes which extend ${ClassNames.androidxListenableWorker}",
                symbol = clazz
            )
            return null
        }

        val constructors = clazz.getConstructors().toList()
        val assistedConstructor = constructors.singleOrNull()

        // Mirror Metro's semantics: an `@AssistedInject` annotation on the class itself
        // applies to the single primary constructor, so either placement is accepted.
        val assistedInjectOnClass = clazz.isAnnotationPresent(ClassNames.assistedInject)
        val assistedInjectOnConstructor =
            assistedConstructor?.isAnnotationPresent(ClassNames.assistedInject) == true

        if (assistedConstructor == null ||
            (!assistedInjectOnClass && !assistedInjectOnConstructor)
        ) {
            logger.error(
                message = "Worker class, which is annotated `@SealantWorker`, must have " +
                        "exactly one constructor and it must be annotated with `@AssistedInject`",
                symbol = clazz
            )
            return null
        }

        val appContextParam = assistedConstructor.parameters.firstOrNull { param ->
            param.isAnnotationPresent(ClassNames.assisted) &&
                    param.type.resolve().toClassName() == ClassNames.androidContext &&
                    param.name?.asString() == "appContext"
        }
        val paramsParam = assistedConstructor.parameters.firstOrNull { param ->
            param.isAnnotationPresent(ClassNames.assisted) &&
                    param.type.resolve().toClassName() == ClassNames.workerParameters &&
                    param.name?.asString() == "workerParams"
        }
        val assistedParamsCount = assistedConstructor.parameters.count { param ->
            param.isAnnotationPresent(ClassNames.assisted)
        }

        if (appContextParam == null || paramsParam == null || assistedParamsCount != 2) {
            logger.error(
                message = "Your constructor which is annotated `@AssistedInject` must have " +
                        "exactly 2 parameters annotated `@Assisted`: " +
                        "`appContext` with type ${ClassNames.androidContext} and " +
                        "`workerParams` with type ${ClassNames.workerParameters}",
                symbol = clazz
            )
            return null
        }

        val origClassName = clazz.toClassName()
        val scopeClassName = clazz.scope().toClassName()

        val afNameStr = "${origClassName.simpleName}_AssistedFactory"
        val afClassName = ClassName(origClassName.packageName, afNameStr)

        val fileName = "${origClassName.simpleName}_Creation"
        val fileNode = clazz.requireContainingFile()

        return SealantFileSpec(origClassName.packageName, fileName) {
            // Генерируем Assisted Factory интерфейс
            addType(buildAssistedFactory(origClassName, afClassName, fileNode))

            // Генерируем Binds Module
            addType(buildBindsModule(origClassName, afClassName, scopeClassName, fileNode))
        }
    }

    /**
     * Generates the Worker Assisted Factory.
     * * Generates an interface annotated with `@AssistedFactory`. This acts as a factory
     * template, instructing Metro to generate an implementation that combines the runtime
     * parameters (`Context`, `WorkerParameters`) with dependencies from the graph to
     * create the target `<Worker>`.
     * * Output example:
     * ```kotlin
     * @AssistedFactory
     * public interface <Worker>_AssistedFactory : WorkerAssistedFactory<<Worker>>
     * ```
     */
    private fun buildAssistedFactory(
        origClassName: ClassName,
        afClassName: ClassName,
        fileNode: KSFile
    ): TypeSpec {
        return InterfaceSpec(afClassName) {
            addAnnotation(AnnotationSpec(ClassNames.assistedFactory))
            addSuperinterface(ClassNames.workerAssistedFactory.parameterizedBy(origClassName))

            addOriginatingKSFile(fileNode)
        }
    }

    /**
     * Generates the Binds Module for the Factory Map.
     * * Contributes a binding module to the target `<Scope>`. It binds the generated
     * assisted factory into a Metro Multibinding Map using a string key corresponding
     * to the Worker's fully qualified class name. A custom Metro-aware `WorkerFactory`
     * will use this map (identified by `@SealantWorkerAssistedFactoryMap`) to locate
     * the correct factory and instantiate the Worker at runtime.
     * * Output example:
     * ```kotlin
     * @BindingContainer
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
    private fun buildBindsModule(
        origClassName: ClassName,
        afClassName: ClassName,
        scopeClassName: ClassName,
        fileNode: KSFile,
    ): TypeSpec {
        val bmNameStr = "${origClassName.simpleName}_BindsModule"
        val bmClassName = ClassName(origClassName.packageName, bmNameStr)

        return InterfaceSpec(bmClassName) {
            addAnnotation(ClassNames.bindingContainer)
            addContributesToAnnotation(scopeClassName)

            addFunction(FunSpec("bind") {
                addAnnotation(ClassNames.binds)
                addAnnotation(ClassNames.intoMap)
                addAnnotation(AnnotationSpec(ClassNames.stringKey) {
                    addMember("%S", origClassName.reflectionName().replace("..", "."))
                })
                addAnnotation(ClassNames.sealantWorkerAssistedFactoryMap)
                addModifiers(KModifier.ABSTRACT)
                addParameter(ParameterSpec("instance", afClassName))
                returns(ClassNames.workerAssistedFactoryOutListenableWorker)
            })

            addOriginatingKSFile(fileNode)
        }
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
