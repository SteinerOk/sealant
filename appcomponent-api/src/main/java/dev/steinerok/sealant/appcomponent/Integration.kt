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
import dagger.MembersInjector
import dev.steinerok.sealant.maintenance.internal.InternalSealantApi

/**
 *
 */
public typealias SealantActivityInjectorsMap = Map<Class<out ComponentActivity>, SealantInjector<*>>

/**
 *
 */
public typealias SealantOtherInjectorsMap = Map<Class<*>, SealantInjector<*>>

/**
 *
 */
public interface SealantInjector<T : Any> {

    /**  */
    @InternalSealantApi
    public val injector: MembersInjector<T>

    /**  */
    @OptIn(InternalSealantApi::class)
    public fun inject(target: T): Unit = injector.injectMembers(target)
}

/**
 *
 */
public interface SealantInjectorsOwner {

    /**  */
    public fun activityInjectors(): SealantActivityInjectorsMap

    /**  */
    public fun otherInjectors(): SealantOtherInjectorsMap
}

/**
 *
 */
public interface SealantInjectable<T : Any> {

    /**  */
    public fun getInjectorsMap(owner: SealantInjectorsOwner): Map<Class<out T>, SealantInjector<*>>
}
