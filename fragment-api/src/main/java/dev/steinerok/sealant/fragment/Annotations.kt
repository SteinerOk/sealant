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
package dev.steinerok.sealant.fragment

import androidx.fragment.app.Fragment
import dagger.MapKey
import dev.steinerok.sealant.maintenance.internal.InternalSealantApi
import kotlin.reflect.KClass

/**
 *
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
public annotation class ContributesFragment(

    /**
     * The scope in which to include this module.
     */
    val scope: KClass<out Any>,

    /**
     * This contributed module will replace these contributed classes. The array is allowed to
     * include other contributed bindings, multibindings and Dagger modules. All replaced classes
     * must use the same scope.
     */
    val replaces: Array<KClass<out Any>> = [],
)

/**
 * A [MapKey] annotation for maps with [KClass] of [Fragment] keys.
 *
 * Note this was designed to be used only with [SealantFragmentFactory].
 */
@InternalSealantApi
@MapKey
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER
)
@Retention(AnnotationRetention.RUNTIME)
public annotation class FragmentKey(

    /**
     *
     */
    val value: KClass<out Fragment>,
)
