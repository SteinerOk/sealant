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

package dev.steinerok.sealant.fragment.compiler.embedded

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.MAP
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.WildcardTypeName
import com.squareup.kotlinpoet.jvm.jvmSuppressWildcards
import dev.steinerok.sealant.compiler.ClassNames
import dev.steinerok.sealant.compiler.embedded.FqNames
import org.jetbrains.kotlin.name.FqName

private const val componentPkg = "dev.steinerok.sealant.fragment"

internal const val integrationPkg = "sealant.integration.fragment"

internal const val featureName = "SealantFragment"

internal val FqNames.contributesFragment
    get() = FqName("$componentPkg.ContributesFragment")

internal val ClassNames.sealantFragmentFactory
    get() = ClassName(componentPkg, "SealantFragmentFactory")

internal val ClassNames.sealantFragmentFactoryOwner
    get() = sealantFragmentFactory.nestedClass("Owner")

internal val ClassNames.fragmentKey
    get() = ClassName(componentPkg, "FragmentKey")

internal val ClassNames.javaClazzOutFragment
    get() = javaClazz.parameterizedBy(WildcardTypeName.producerOf(androidxFragment))

internal val ClassNames.fragmentMap
    get() = MAP.parameterizedBy(ClassNames.javaClazzOutFragment, androidxFragment)

internal val ClassNames.fragmentProviderMap
    get() = MAP.parameterizedBy(
        ClassNames.javaClazzOutFragment,
        provider.parameterizedBy(androidxFragment).jvmSuppressWildcards()
    )
