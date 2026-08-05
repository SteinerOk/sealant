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
package dev.steinerok.sealant.viewmodel.compiler.ksp

import com.google.auto.service.AutoService
import com.google.devtools.ksp.getConstructors
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.Modifier
import com.google.devtools.ksp.validate
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.ksp.addOriginatingKSFile
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.writeTo
import dev.steinerok.sealant.compiler.AnnotationSpec
import dev.steinerok.sealant.compiler.ClassNames
import dev.steinerok.sealant.compiler.FunSpec
import dev.steinerok.sealant.compiler.InterfaceSpec
import dev.steinerok.sealant.compiler.ObjectSpec
import dev.steinerok.sealant.compiler.ParameterSpec
import dev.steinerok.sealant.compiler.SealantFeature
import dev.steinerok.sealant.compiler.addContributesToAnnotation
import dev.steinerok.sealant.compiler.buildVmScopeClassName
import dev.steinerok.sealant.compiler.ksp.SealantFileSpec
import dev.steinerok.sealant.compiler.ksp.getSymbolsWithAnnotation
import dev.steinerok.sealant.compiler.ksp.hasSealantFeatureForScope
import dev.steinerok.sealant.compiler.ksp.implements
import dev.steinerok.sealant.compiler.ksp.requireContainingFile
import dev.steinerok.sealant.compiler.ksp.scope
import dev.steinerok.sealant.compiler.ksp.simpleValidatePredicate

/**
 * Generates per-ViewModel bindings required by Sealant's custom `ViewModelProvider.Factory`.
 *
 * The overall structure is inspired by Hilt's internal ViewModel code generation, but adapted to
 * Sealant scopes and to class-keyed multibindings instead of string-keyed ones.
 *
 * * Architecture Note (Hilt Comparison):
 * This setup achieves the same dependency resolution as Hilt's `@HiltViewModel`
 * codegen. However, there are two key distinctions:
 * - Scope separation: Hilt splits these between `ViewModelComponent` (for the map)
 * and `ActivityRetainedComponent` (for the key set).
 * - Key types: Hilt uses string-based keys (`@StringKey("pkg.$")`), whereas this
 * implementation utilizes direct class types (`KClass<out ViewModel>`) for enhanced
 * type safety within the designated Sealant scopes.
 *
 * Related with Hilt codegen:
 * ```java
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
public class ViewModelCreationSymbolProcessor(
    private val codeGenerator: CodeGenerator,
    @Suppress("unused") private val options: Map<String, String>,
    private val logger: KSPLogger,
) : SymbolProcessor {

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val (validSymbols, invalidSymbols) = resolver
            .getSymbolsWithAnnotation(ClassNames.contributesViewModel)
            .filterIsInstance<KSClassDeclaration>()
            .partition { symbol -> symbol.validate(simpleValidatePredicate) }

        validSymbols
            .filter { it.scope().hasSealantFeatureForScope(SealantFeature.ViewModel) }
            .forEach { symbol ->
                generateByProcessor(symbol)?.writeTo(
                    codeGenerator = codeGenerator,
                    aggregating = false,
                )
            }

        return invalidSymbols
    }

    private fun generateByProcessor(clazz: KSClassDeclaration): FileSpec? {
        if (clazz.classKind != ClassKind.CLASS) {
            logger.error(
                message = "`@ContributesViewModel` is only applicable to classes.",
                symbol = clazz
            )
            return null
        }

        val forbiddenModifiers = setOf(Modifier.PRIVATE, Modifier.INNER, Modifier.ABSTRACT)
        val foundForbidden = clazz.modifiers.intersect(forbiddenModifiers)
        if (foundForbidden.isNotEmpty()) {
            val modifiersStr = foundForbidden.joinToString(" and ") { it.name.lowercase() }
            logger.error(
                message = "Class annotated with `@ContributesViewModel` cannot be $modifiersStr.",
                symbol = clazz
            )
            return null
        }

        if (clazz.typeParameters.isNotEmpty()) {
            logger.error(
                message = "Class annotated with `@ContributesViewModel` cannot have type parameters (generics).",
                symbol = clazz
            )
            return null
        }

        if (!clazz.implements(ClassNames.androidxViewModel)) {
            logger.error(
                message = "The annotation `@SealantViewModel` can only be applied " +
                        "to classes which extend ${ClassNames.androidxViewModel}",
                symbol = clazz
            )
            return null
        }

        val hasScope = clazz.annotations.any { annotation ->
            annotation.annotationType.resolve().declaration.annotations.any { meta ->
                meta.shortName.asString() == "Scope"
            }
        }
        if (hasScope) {
            logger.error(
                message = "ViewModel classes must not be scoped. The lifecycle is managed by Android.",
                symbol = clazz
            )
            return null
        }

        val constructors = clazz.getConstructors().toList()
        val injectConstructors = constructors.filter { constructors ->
            constructors.annotations.any { annotation ->
                annotation.shortName.asString() == ClassNames.inject.simpleName
            }
        }
        val assistedConstructors = constructors.filter { constructors ->
            constructors.annotations.any { annotation ->
                annotation.shortName.asString() == ClassNames.assistedInject.simpleName
            }
        }

        val totalInjectedConstructors = injectConstructors.size + assistedConstructors.size
        if (totalInjectedConstructors == 0) {
            logger.error(
                message = "ViewModel must contain exactly one constructor annotated with `@Inject` or `@AssistedInject`.",
                symbol = clazz
            )
            return null
        }
        if (totalInjectedConstructors > 1) {
            logger.error(
                message = "ViewModel cannot have multiple constructors annotated with `@Inject` or `@AssistedInject`.",
                symbol = clazz
            )
            return null
        }

        val annotation = clazz.annotations.firstOrNull { annotation ->
            annotation.shortName.asString() == ClassNames.contributesViewModel.simpleName
        } ?: run {
            logger.error("Missing `@ContributesViewModel` annotation.", clazz)
            return null
        }

        val factoryKsType = annotation.arguments.firstOrNull { argument ->
            argument.name?.asString() == "assistedFactory"
        }?.value as? KSType
        val factoryDecl = factoryKsType?.declaration as? KSClassDeclaration
        val hasFactorySpecified = factoryDecl != null &&
                factoryDecl.simpleName.asString() != ClassNames.nothing.simpleName
        val isAssistedInject = assistedConstructors.isNotEmpty()

        if (isAssistedInject) {
            if (!hasFactorySpecified) {
                logger.error(
                    message = "ViewModel is annotated with `@AssistedInject` but did not specify an `assistedFactory` in `@ContributesViewModel`.",
                    symbol = clazz
                )
                return null
            }

            val factoryHasAssistedFactoryAnnotation = factoryDecl.annotations.any { annotation ->
                annotation.shortName.asString() == ClassNames.assistedFactory.simpleName
            }

            if (!factoryHasAssistedFactoryAnnotation) {
                logger.error(
                    message = "ViewModel's `assistedFactory` must be annotated with `@AssistedFactory`.",
                    symbol = factoryDecl
                )
                return null
            }
        } else {
            if (hasFactorySpecified) {
                logger.error(
                    message = "ViewModel is annotated with `@Inject` but specified an `assistedFactory` in `@ContributesViewModel`. Please remove it.",
                    symbol = clazz
                )
                return null
            }
        }

        val assistedFactoryClassName = if (isAssistedInject) factoryDecl?.toClassName() else null

        val origClassName = clazz.toClassName()
        val origShortName = origClassName.simpleName

        val scopeClassName = clazz.scope().toClassName()
        val vmScopeClassName = buildVmScopeClassName(scopeClassName)

        val fileName = "${origShortName}_Creation"
        val fileNode = clazz.requireContainingFile()

        return SealantFileSpec(origClassName.packageName, fileName) {
            // Генерируем ViewModel Key Set Module
            addType(buildKeyModule(origClassName, origShortName, scopeClassName, fileNode))

            // BindsModule зависит от того, Assisted это или обычный Inject
            if (isAssistedInject) {
                addType(
                    buildAssistedBindsModule(
                        origClassName,
                        origShortName,
                        assistedFactoryClassName!!,
                        vmScopeClassName,
                        fileNode
                    )
                )
            } else {
                addType(buildBindsModule(origClassName, origShortName, vmScopeClassName, fileNode))
            }
        }
    }

    /**
     * Generates the ViewModel Key Set Module.
     * * Contributes to the main `<Scope>` (conceptually similar to Hilt's
     * `ActivityRetainedComponent`). It provides the specific ViewModel's class
     * into a Metro Set (`@SealantViewModelMap.KeySet`). This allows the dependency
     * graph to maintain a registry of all available ViewModels, which can be used
     * for graph validation or by the factory to verify support before instantiation.
     * * Output example:
     * ```kotlin
     * @BindingContainer
     * @ContributesTo(scope = <Scope>::class)
     * public object <ViewModel>_KeyModule {
     *   @Provides
     *   @IntoSet
     *   @SealantViewModelMap.KeySet
     *   public fun provide<ViewModel>Key(): KClass<out ViewModel> = <ViewModel>::class
     * }
     * ```
     */
    private fun buildKeyModule(
        origClassName: ClassName,
        origShortName: String,
        scopeClassName: ClassName,
        fileNode: KSFile,
    ): TypeSpec {
        val kmNameStr = "${origShortName}_KeyModule"

        return ObjectSpec(ClassName(origClassName.packageName, kmNameStr)) {
            addContributesToAnnotation(scopeClassName)
            addAnnotation(ClassNames.bindingContainer)

            addFunction(FunSpec("provide${origShortName}Key") {
                addAnnotation(ClassNames.provides)
                addAnnotation(ClassNames.intoSet)
                addAnnotation(ClassNames.sealantViewModelSupportKeySet)
                returns(ClassNames.kotlinClazzOutViewModel)
                addStatement("return %T::class", origClassName)
            })

            addOriginatingKSFile(fileNode)
        }
    }

    /**
     * Generates the ViewModel Binds Module.
     * * Contributes directly to the sub-scope `<Scope>_ViewModel::class`. It binds
     * the concrete `<ViewModel>` instance to the base `ViewModel` type inside a
     * Multibinding Map. This map (`@SealantViewModelMap`) is then injected into
     * the custom factory to resolve and create the correct ViewModel instance at runtime.
     * * Output example:
     * ```kotlin
     * @BindingContainer
     * @ContributesTo(scope = <Scope>_ViewModel::class)
     * public interface <ViewModel>_BindsModule {
     *   @Binds
     *   @IntoMap
     *   @ViewModelKey
     *   @SealantViewModelMap
     *   public fun bind(instance: <ViewModel>): ViewModel
     * }
     * ```
     */
    private fun buildBindsModule(
        origClassName: ClassName,
        origShortName: String,
        vmScopeClassName: ClassName,
        fileNode: KSFile,
    ): TypeSpec {
        val bmNameStr = "${origShortName}_BindsModule"

        return InterfaceSpec(ClassName(origClassName.packageName, bmNameStr)) {
            addContributesToAnnotation(vmScopeClassName)
            addAnnotation(ClassNames.bindingContainer)

            addFunction(FunSpec("bind") {
                addAnnotation(ClassNames.binds)
                addAnnotation(ClassNames.intoMap)
                addAnnotation(ClassNames.viewModelKey)
                addAnnotation(ClassNames.sealantViewModelMap)
                addModifiers(KModifier.ABSTRACT)
                addParameter(ParameterSpec("instance", origClassName))
                returns(ClassNames.androidxViewModel)
            })

            addOriginatingKSFile(fileNode)
        }
    }

    /**
     * Generates the ViewModel Assisted Binds Module.
     * * Contributes directly to the sub-scope `<Scope>_ViewModel::class`. Unlike the
     * standard binds module, this binds the user-defined `@AssistedFactory` interface
     * (rather than the ViewModel instance itself) into the Multibinding Map as `Any`.
     * * This allows the custom `ViewModelProvider.Factory` to retrieve the factory
     * from the map (`@SealantViewModelAssistedMap`) at runtime, cast it to the correct type,
     * and instantiate the ViewModel with the necessary dynamic arguments.
     * * Output example:
     * ```kotlin
     * @BindingContainer
     * @ContributesTo(scope = <Scope>_ViewModel::class)
     * public interface <ViewModel>_AssistedBindsModule {
     *   @Binds
     *   @IntoMap
     *   @ViewModelKey(<ViewModel>::class)
     *   @SealantViewModelAssistedMap
     *   public fun bindFactory(factory: <ViewModel>_AssistedFactory): Any
     * }
     * ```
     */
    private fun buildAssistedBindsModule(
        origClassName: ClassName,
        origShortName: String,
        factoryClassName: ClassName,
        vmScopeClassName: ClassName,
        fileNode: KSFile,
    ): TypeSpec {
        val abmNameStr = "${origShortName}_AssistedBindsModule"

        return InterfaceSpec(ClassName(origClassName.packageName, abmNameStr)) {
            addContributesToAnnotation(vmScopeClassName)
            addAnnotation(ClassNames.bindingContainer)

            addFunction(FunSpec("bindFactory") {
                addAnnotation(ClassNames.binds)
                addAnnotation(ClassNames.intoMap)
                addAnnotation(AnnotationSpec(ClassNames.viewModelKey) {
                    addMember("%T::class", origClassName)
                })
                addAnnotation(ClassNames.sealantViewModelAssistedMap)
                addModifiers(KModifier.ABSTRACT)
                addParameter(ParameterSpec("factory", factoryClassName))
                returns(ClassNames.any)
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
            return ViewModelCreationSymbolProcessor(
                codeGenerator = environment.codeGenerator,
                options = environment.options,
                logger = environment.logger,
            )
        }
    }
}
