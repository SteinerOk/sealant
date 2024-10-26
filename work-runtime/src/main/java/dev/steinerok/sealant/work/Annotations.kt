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
package dev.steinerok.sealant.work

import androidx.work.ListenableWorker
import dev.steinerok.sealant.core.internal.InternalSealantApi
import javax.inject.Qualifier
import kotlin.reflect.KClass

/**
 * Adds the annotated [ListenableWorker] to Dagger's graph via Sealant.
 * The corresponding Worker can then be created using [SealantWorkerFactory].
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
public annotation class ContributesWorker(

    /** The scope in which to include this contributed Worker */
    val scope: KClass<out Any>,
)

/**
 * Qualifier for the internal `Map<String, WorkerAssistedFactory<out ListenableWorker>>`
 * used in Sealant's [ListenableWorker] multi-binding.
 *
 * This is an internal API and should not be referenced directly.
 */
@InternalSealantApi
@Qualifier
@Retention(AnnotationRetention.RUNTIME)
@Target(
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.FIELD,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.VALUE_PARAMETER
)
public annotation class SealantWorkerAssistedFactoryMap
