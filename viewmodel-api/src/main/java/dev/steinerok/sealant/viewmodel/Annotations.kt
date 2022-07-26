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
package dev.steinerok.sealant.viewmodel

import androidx.annotation.Keep
import androidx.lifecycle.ViewModel
import dagger.MapKey
import dev.steinerok.sealant.maintenance.internal.InternalSealantApi
import javax.inject.Qualifier
import kotlin.reflect.KClass

/**
 *
 */
@Keep
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
public annotation class ContributesViewModel(

    /** The scope from which to pull the annotated class's dependencies. */
    val scope: KClass<out Any>
)

/**
 *
 */
@Keep
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
public annotation class ContributesToViewModel(

    /** The scope from which to pull the annotated class's dependencies. */
    val scope: KClass<out Any>
)

/**
 * Annotation to be applied to a getter or setter function, that is stored in the binary output.
 * A [ViewModelKey] with [KClass] object will be the key in a Map generated by Dagger.
 * The value will be the [ViewModel] to be retrieved based on the key.
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
public annotation class ViewModelKey(

    /**
     * The [ViewModel] class used as key.
     *
     * @return the class.
     */
    val value: KClass<out ViewModel>
)

/**
 *
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
public annotation class SealantViewModelMap {

    /**
     *
     */
    @Qualifier
    @Retention(AnnotationRetention.RUNTIME)
    @Target(
        AnnotationTarget.PROPERTY_GETTER,
        AnnotationTarget.FIELD,
        AnnotationTarget.VALUE_PARAMETER,
        AnnotationTarget.FUNCTION
    )
    public annotation class KeySet

    /**
     *
     */
    @Qualifier
    @Retention(AnnotationRetention.RUNTIME)
    @Target(
        AnnotationTarget.PROPERTY_GETTER,
        AnnotationTarget.FIELD,
        AnnotationTarget.VALUE_PARAMETER,
        AnnotationTarget.FUNCTION
    )
    public annotation class SubcomponentMap
}
