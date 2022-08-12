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

import kotlin.reflect.KClass

/**
 *
 */
public typealias InjectorsResolver = (scope: KClass<out Any>, injectable: Any) -> SealantInjectorsOwner

/**
 *
 */
public fun injectViaSealant(
    injectable: SealantInjectable<*>,
    injectorsResolver: InjectorsResolver
) {
    val injectWith = requireNotNull(injectable::class.java.getAnnotation(InjectWith::class.java)) {
        "Injectable class is not annotated @InjectWith, but must"
    }

    val injectScope = injectWith.scope
    val injectorsOwner = injectorsResolver(injectScope, injectable)
    val injectorsMap = injectable.getInjectorsMap(injectorsOwner)

    @Suppress("UNCHECKED_CAST")
    val injector = requireNotNull(injectorsMap[injectable.javaClass]) {
        "Returned injector is null for injectable: $injectable with scope: ${injectWith.scope}"
    } as SealantInjector<Any>

    injector.inject(injectable)
}
