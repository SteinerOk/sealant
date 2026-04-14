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

package dev.steinerok.sealant.appcomponent.compiler.ksp

import com.squareup.kotlinpoet.ClassName
import dev.steinerok.sealant.compiler.ClassNames

internal const val featureName = "SealantAppcomponent"

private const val componentPkg = "dev.steinerok.sealant.appcomponent"

internal const val integrationPkg = "sealant.integration.appcomponent"

internal val ClassNames.injectWith
    get() = ClassName(componentPkg, "InjectWith")

internal val ClassNames.sealantInjector
    get() = ClassName(componentPkg, "SealantInjector")

internal val ClassNames.sealantInjectorsOwner
    get() = ClassName(componentPkg, "SealantInjectorsOwner")

internal val ClassNames.sealantActivityInjectorsMap
    get() = ClassName(componentPkg, "SealantActivityInjectorsMap")

internal val ClassNames.sealantBroadcastReceiverInjectorsMap
    get() = ClassName(componentPkg, "SealantBroadcastReceiverInjectorsMap")

internal val ClassNames.sealantContentProviderInjectorsMap
    get() = ClassName(componentPkg, "SealantContentProviderInjectorsMap")

internal val ClassNames.sealantServiceInjectorsMap
    get() = ClassName(componentPkg, "SealantServiceInjectorsMap")

internal val ClassNames.activityKey
    get() = ClassName(componentPkg, "ActivityKey")

internal val ClassNames.broadcastReceiverKey
    get() = ClassName(componentPkg, "BroadcastReceiverKey")

internal val ClassNames.contentProviderKey
    get() = ClassName(componentPkg, "ContentProviderKey")

internal val ClassNames.serviceKey
    get() = ClassName(componentPkg, "ServiceKey")
