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
@file:Suppress("UnusedReceiverParameter")

package dev.steinerok.sealant.viewmodel.compiler.embedded

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.MAP
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.SET
import com.squareup.kotlinpoet.WildcardTypeName
import dev.steinerok.sealant.compiler.ClassNames
import dev.steinerok.sealant.compiler.embedded.FqNames
import org.jetbrains.kotlin.name.FqName

internal const val featureName = "SealantViewModel"

private const val componentPkg = "dev.steinerok.sealant.viewmodel"

internal const val integrationPkg = "sealant.integration.viewmodel"

internal val FqNames.contributesViewModel
    get() = FqName("$componentPkg.ContributesViewModel")

internal val FqNames.contributesToViewModel
    get() = FqName("$componentPkg.ContributesToViewModel")

internal val ClassNames.javaClazzOutViewModel
    get() = javaClazz.parameterizedBy(WildcardTypeName.producerOf(androidxViewModel))

internal val ClassNames.viewModelClassSet
    get() = SET.parameterizedBy(ClassNames.javaClazzOutViewModel)

internal val ClassNames.viewModelMap
    get() = MAP.parameterizedBy(ClassNames.javaClazzOutViewModel, androidxViewModel)

internal val ClassNames.sealantViewModelScope
    get() = ClassName(componentPkg, "SealantViewModelScope")

internal val ClassNames.sealantViewModelMap
    get() = ClassName(componentPkg, "SealantViewModelMap")

internal val ClassNames.sealantViewModelMapKeySet
    get() = sealantViewModelMap.nestedClass("KeySet")

internal val ClassNames.sealantViewModelMapSubcomponentMap
    get() = sealantViewModelMap.nestedClass("SubcomponentMap")

internal val ClassNames.viewModelKey
    get() = ClassName(componentPkg, "ViewModelKey")

internal val ClassNames.sealantViewModelSubcomponent
    get() = ClassName(componentPkg, "SealantViewModelSubcomponent")

internal val ClassNames.sealantViewModelSubcomponentFactory
    get() = sealantViewModelSubcomponent.nestedClass("Factory")

internal val ClassNames.sealantViewModelSubcomponentParent
    get() = sealantViewModelSubcomponent.nestedClass("Parent")

internal val ClassNames.viewModelFactoriesOwner
    get() = ClassName(componentPkg, "ViewModelFactoriesOwner")

internal val ClassNames.sealantViewModelFactoryCreator
    get() = ClassName(componentPkg, "SealantViewModelFactoryCreator")

internal val ClassNames.sealantViewModelFactoryCreatorOwner
    get() = sealantViewModelFactoryCreator.nestedClass("Owner")
