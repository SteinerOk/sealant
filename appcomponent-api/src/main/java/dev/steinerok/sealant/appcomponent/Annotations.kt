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
package dev.steinerok.sealant.appcomponent

import androidx.activity.ComponentActivity
import dagger.MapKey
import dev.steinerok.sealant.maintenance.internal.InternalSealantApi
import kotlin.reflect.KClass

/**
 * Annotates a member-injected class to indicate the scope which will provide its dependencies
 * for all Android types like Activities, Services, etc.
 *
 * Usage:
 * ```kotlin
 * @InjectWith(SomeAnvilScope::class)
 * class MainActivity : ComponentActivity {
 *
 * }
 * ```
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
public annotation class InjectWith(

    /** The scope from which to pull the annotated class's dependencies. */
    val scope: KClass<out Any>
)

/**
 *
 */
@InternalSealantApi
@MapKey
@Target(
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER
)
@Retention(AnnotationRetention.RUNTIME)
public annotation class ActivityKey(

    /**  */
    val value: KClass<out ComponentActivity>
)
