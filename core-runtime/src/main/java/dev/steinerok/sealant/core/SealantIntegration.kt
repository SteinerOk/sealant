/*
 * Copyright 2024 Ihor Kushnirenko
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
 * Marks an integration entry point for one or more Sealant-enabled scopes.
 *
 * Sealant processors scan this annotation to generate the top-level infrastructure that exposes
 * factories, injector maps and other scope-level bindings from the owning component.
 *
 * This is typically placed on the `Application` class.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
public annotation class SealantIntegration(

    /** Scopes that should expose generated Sealant integrations from this entry point. */
    val scopes: Array<KClass<out Any>>,
)
