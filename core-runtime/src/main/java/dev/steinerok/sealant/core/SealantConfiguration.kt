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
package dev.steinerok.sealant.core

import kotlin.reflect.KClass

/**
 * Declares which Sealant integrations should be generated for the annotated scope.
 *
 * Apply this annotation to a scope marker class that participates in your Anvil graph.
 * Sealant uses it as the source of truth for deciding which feature-specific processors
 * should contribute bindings for that scope.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
public annotation class SealantConfiguration(

    /** Enables generation of member-injection infrastructure for Android components. */
    val addAppComponentSupport: Boolean,

    /** Enables generation of ViewModel-specific multibindings and subcomponents. */
    val addViewModelSupport: Boolean,

    /** Enables generation of fragment bindings and the corresponding [androidx.fragment.app.FragmentFactory]. */
    val addFragmentSupport: Boolean,

    /** Enables generation of WorkManager bindings and [androidx.work.WorkerFactory] integration. */
    val addWorkSupport: Boolean,

    /**
     * Parent scope used to model Sealant feature inheritance.
     *
     * When specified, child scopes can reuse infrastructure from an ancestor scope instead of
     * generating duplicate integration modules.
     */
    val parentScope: KClass<out Any> = Unit::class,
)
