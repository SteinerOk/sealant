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

package dev.steinerok.sealant.work.codegen

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.MAP
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.STRING
import com.squareup.kotlinpoet.WildcardTypeName
import com.squareup.kotlinpoet.jvm.jvmSuppressWildcards
import dev.steinerok.sealant.compiler.ClassNames
import dev.steinerok.sealant.compiler.embedded.FqNames
import org.jetbrains.kotlin.name.FqName

internal const val featureName = "SealantWork"

private const val componentPkg = "dev.steinerok.sealant.work"

internal const val integrationPkg = "sealant.integration.work"

internal val FqNames.contributesWorker
    get() = FqName("$componentPkg.ContributesWorker")

internal val ClassNames.workerParameters
    get() = ClassName("androidx.work", "WorkerParameters")

internal val ClassNames.sealantWorkerFactory
    get() = ClassName(componentPkg, "SealantWorkerFactory")

internal val ClassNames.sealantWorkerFactoryOwner
    get() = sealantWorkerFactory.nestedClass("Owner")

internal val ClassNames.workerAssistedFactory
    get() = ClassName(componentPkg, "WorkerAssistedFactory")

internal val ClassNames.workerAssistedFactoryOutListenableWorker
    get() = workerAssistedFactory.parameterizedBy(
        WildcardTypeName.producerOf(androidxListenableWorker)
    )

internal val ClassNames.workerAssistedFactoryMap
    get() = MAP.parameterizedBy(STRING, workerAssistedFactoryOutListenableWorker)

internal val ClassNames.workerAssistedFactoryProviderMap
    get() = MAP.parameterizedBy(
        STRING,
        provider.parameterizedBy(workerAssistedFactoryOutListenableWorker).jvmSuppressWildcards()
    )

internal val ClassNames.sealantWorkerAssistedFactoryMap
    get() = ClassName(componentPkg, "SealantWorkerAssistedFactoryMap")
