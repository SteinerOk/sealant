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
package dev.steinerok.sealant.appcomponent.compiler.ksp

import com.google.auto.service.AutoService
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
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.STAR
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.ksp.addOriginatingKSFile
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.writeTo
import dev.steinerok.sealant.compiler.AnnotationSpec
import dev.steinerok.sealant.compiler.ClassNames
import dev.steinerok.sealant.compiler.ClassSpec
import dev.steinerok.sealant.compiler.FunSpec
import dev.steinerok.sealant.compiler.InterfaceSpec
import dev.steinerok.sealant.compiler.ObjectSpec
import dev.steinerok.sealant.compiler.ParameterSpec
import dev.steinerok.sealant.compiler.PropertySpec
import dev.steinerok.sealant.compiler.SealantFeature
import dev.steinerok.sealant.compiler.addContributesToAnnotation
import dev.steinerok.sealant.compiler.addPrimaryInjectConstructor
import dev.steinerok.sealant.compiler.ksp.SealantFileSpec
import dev.steinerok.sealant.compiler.ksp.SealantOptions
import dev.steinerok.sealant.compiler.ksp.getSymbolsWithAnnotation
import dev.steinerok.sealant.compiler.ksp.hasSealantFeatureForScope
import dev.steinerok.sealant.compiler.ksp.implements
import dev.steinerok.sealant.compiler.ksp.requireContainingFile
import dev.steinerok.sealant.compiler.ksp.scope

/**
 * Description of the injection component generation for the target class.
 * This generator creates a set of classes for seamless integration with Dagger+Anvil/Metro,
 * providing dependency injection and registering the injector in the graph.
 */
public class AppComponentInjectionSymbolProcessor(
    private val codeGenerator: CodeGenerator,
    private val options: Map<String, String>,
    private val logger: KSPLogger,
) : SymbolProcessor {

    private val sealantOptions = SealantOptions.load(options, logger)

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val (validSymbols, invalidSymbols) = resolver
            .getSymbolsWithAnnotation(ClassNames.injectWith)
            .filterIsInstance<KSClassDeclaration>()
            .partition { symbol -> symbol.validate() }

        validSymbols
            .filter { annotated ->
                annotated
                    .scope()
                    .hasSealantFeatureForScope(SealantFeature.AppComponent)
            }
            .forEach { symbol ->
                generateByProcessor(symbol)?.writeTo(
                    codeGenerator = codeGenerator,
                    aggregating = false,
                )
            }

        return invalidSymbols
    }

    private fun generateByProcessor(clazz: KSClassDeclaration): FileSpec? {
        val origClassName = clazz.toClassName()
        val origShortName = clazz.simpleName.asString()

        val injectionKey = when {
            clazz.implements(ClassNames.androidActivity) -> ClassNames.activityKey
            clazz.implements(ClassNames.androidBroadcastReceiver) -> ClassNames.broadcastReceiverKey
            clazz.implements(ClassNames.androidService) -> ClassNames.serviceKey
            clazz.implements(ClassNames.androidContentProvider) -> ClassNames.contentProviderKey
            else -> {
                logger.error(
                    message = "Unsupported injectable type: ${clazz.qualifiedName?.asString()}",
                    symbol = clazz,
                )
                return null
            }
        }
        val scopeClassName = clazz.scope().toClassName()

        val injectorNameStr = "${origShortName}_${ClassNames.sealantInjector.simpleName}"
        val injectorClassName = ClassName(origClassName.packageName, injectorNameStr)

        val fileName = "${origShortName}_Injection"
        val fileNode = clazz.requireContainingFile()

        return SealantFileSpec(origClassName.packageName, fileName) {
            // Генерируем Main Injector Class
            addType(buildInjectorClass(origClassName, injectorClassName, fileNode))

            // Генерируем Provides Module для Metro (Metro not generate Dagger MembersInjector)
            if (sealantOptions.useMetro) {
                addType(
                    buildMetroProvidesModule(
                        origClassName, injectorClassName, scopeClassName, fileNode
                    )
                )
            }

            // Генерируем Binds Module
            addType(
                buildBindsModule(
                    origClassName, injectorClassName, scopeClassName, injectionKey, fileNode
                )
            )
        }
    }

    /**
     * Generates the Main Injector Class.
     * * Implements the base `SealantInjector` interface and encapsulates the injection logic.
     * A `MembersInjector` is passed into the constructor, which performs the actual field injection
     * into the target class.
     * * Output example:
     * ```kotlin
     * public class <Type>_SealantInjector @Inject constructor(
     *     public override val injector: MembersInjector<<Type>>
     * ) : SealantInjector<<Type>>
     * ```
     */
    private fun buildInjectorClass(
        origClassName: ClassName,
        injectorClassName: ClassName,
        fileNode: KSFile,
    ): TypeSpec {
        return ClassSpec(injectorClassName) {
            addSuperinterface(ClassNames.sealantInjector.parameterizedBy(origClassName))
            val miType = ClassNames.membersInjector.parameterizedBy(origClassName)
            addPrimaryInjectConstructor(
                PropertySpec("injector", miType) {
                    addModifiers(KModifier.OVERRIDE)
                }
            )

            addOriginatingKSFile(fileNode)
        }
    }

    /**
     * Generates the Provides Module for the bridge with Dagger MembersInjector (!!! Only for Metro !!!).
     * * Acts as an adapter between the `MetroMembersInjector` and the standard `DaggerMembersInjector`.
     * It allows the Dagger graph to correctly resolve dependencies for `MembersInjector<<Type>>`,
     * which is requested in the constructor of the main injector class.
     * * Output example:
     * ```kotlin
     * @Module
     * @ContributesTo(scope = AppScope::class)
     * public object <Type>_SealantInjector_ProvidesModule {
     *     @Provides
     *     public fun provideDaggerMembersInjector(
     *         metroMembersInjector: MetroMembersInjector<<Type>>
     *     ): DaggerMembersInjector<<Type>> = metroMembersInjector.asDaggerMembersInjector()
     * }
     * ```
     */
    private fun buildMetroProvidesModule(
        origClassName: ClassName,
        injectorClassName: ClassName,
        scopeClassName: ClassName,
        fileNode: KSFile,
    ): TypeSpec {
        val ipmNameStr = "${injectorClassName.simpleName}_ProvidesModule"
        val ipmClassName = ClassName(injectorClassName.packageName, ipmNameStr)

        return ObjectSpec(ipmClassName) {
            addAnnotation(ClassNames.module)
            addContributesToAnnotation(scopeClassName)

            addFunction(FunSpec("provideDaggerMembersInjector") {
                addAnnotation(ClassNames.provides)
                addParameter(
                    ParameterSpec(
                        "metroMembersInjector",
                        ClassName("dev.zacsweers.metro", "MembersInjector")
                            .parameterizedBy(origClassName)
                    )
                )
                returns(ClassNames.membersInjector.parameterizedBy(origClassName))
                addStatement(
                    "return metroMembersInjector.%M()",
                    MemberName("dev.zacsweers.metro.interop.dagger", "asDaggerMembersInjector")
                )
            })

            addOriginatingKSFile(fileNode)
        }
    }

    /**
     * Generates the Binds Module for the injectors map.
     * * Adds the generated injector to the Dagger Multibinding Map.
     * This allows a factory or dispatcher to find the required injector at runtime
     * using the activity key (`ActivityKey`), mapping the `<Type>` to its `AnvilInjector` implementation.
     * * Output example:
     * ```kotlin
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
    private fun buildBindsModule(
        origClassName: ClassName,
        injectorClassName: ClassName,
        scopeClassName: ClassName,
        injectionKey: ClassName,
        fileNode: KSFile,
    ): TypeSpec {
        val ibmNameStr = "${injectorClassName.simpleName}_BindsModule"
        val ibmClassName = ClassName(injectorClassName.packageName, ibmNameStr)

        return InterfaceSpec(ibmClassName) {
            addAnnotation(ClassNames.module)
            addContributesToAnnotation(scopeClassName)

            addFunction(FunSpec("bind") {
                addAnnotation(ClassNames.binds)
                addAnnotation(ClassNames.intoMap)
                addAnnotation(
                    AnnotationSpec(injectionKey) { addMember("%T::class", origClassName) }
                )
                addModifiers(KModifier.ABSTRACT)
                addParameter(ParameterSpec("instance", injectorClassName))
                returns(ClassNames.sealantInjector.parameterizedBy(STAR))
            })

            addOriginatingKSFile(fileNode)
        }
    }

    /**
     * Entry point for KSP to pick up our [SymbolProcessor].
     */
    @AutoService(SymbolProcessorProvider::class)
    public class Provider : SymbolProcessorProvider {
        override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
            return AppComponentInjectionSymbolProcessor(
                codeGenerator = environment.codeGenerator,
                options = environment.options,
                logger = environment.logger,
            )
        }
    }
}
