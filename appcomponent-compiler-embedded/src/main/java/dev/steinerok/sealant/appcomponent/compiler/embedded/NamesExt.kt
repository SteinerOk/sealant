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

package dev.steinerok.sealant.appcomponent.compiler.embedded

import com.squareup.kotlinpoet.ClassName
import dev.steinerok.sealant.compiler.ClassNames
import dev.steinerok.sealant.compiler.embedded.FqNames
import org.jetbrains.kotlin.name.FqName

internal const val featureName = "SealantAppcomponent"

private const val componentPkg = "dev.steinerok.sealant.appcomponent"

internal const val integrationPkg = "sealant.integration.appcomponent"

internal val FqNames.injectWith
    get() = FqName("$componentPkg.InjectWith")

internal val ClassNames.sealantInjector
    get() = ClassName(componentPkg, "SealantInjector")

internal val ClassNames.sealantInjectorsOwner
    get() = ClassName(componentPkg, "SealantInjectorsOwner")

internal val ClassNames.sealantActivityInjectorsMap
    get() = ClassName(componentPkg, "SealantActivityInjectorsMap")

internal val ClassNames.sealantOtherInjectorsMap
    get() = ClassName(componentPkg, "SealantOtherInjectorsMap")

internal val ClassNames.activityKey
    get() = ClassName(componentPkg, "ActivityKey")