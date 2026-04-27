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

import android.app.Activity
import android.app.Service
import android.content.BroadcastReceiver
import android.content.ContentProvider
import dagger.MembersInjector
import dev.steinerok.sealant.core.internal.InternalSealantApi

/** Type alias for the generated map of activity injectors. */
public typealias SealantActivityInjectorsMap = Map<Class<out Activity>, SealantInjector<*>>

/** Type alias for the generated map of broadcast receiver injectors. */
public typealias SealantBroadcastReceiverInjectorsMap = Map<Class<out BroadcastReceiver>, SealantInjector<*>>

/** Type alias for the generated map of content provider injectors. */
public typealias SealantContentProviderInjectorsMap = Map<Class<out ContentProvider>, SealantInjector<*>>

/** Type alias for the generated map of service injectors. */
public typealias SealantServiceInjectorsMap = Map<Class<out Service>, SealantInjector<*>>

/**
 * Wrapper around a generated [MembersInjector].
 *
 * Sealant adds implementations of this interface to Dagger multibinding maps so framework entry
 * points can be injected without depending on generated class names directly.
 */
public interface SealantInjector<T : Any> {

    /** Backing Dagger injector used to assign members on the target instance. */
    @InternalSealantApi
    public val injector: MembersInjector<T>

    /** Injects members into [target] using the generated [injector]. */
    @OptIn(InternalSealantApi::class)
    public fun inject(target: T): Unit = injector.injectMembers(target)
}

/**
 * Exposes all Android-component injector maps generated for a scope.
 *
 * Components contributed to a Sealant-enabled scope typically implement this interface through a
 * generated owner type.
 */
public interface SealantInjectorsOwner {

    /** Returns the injector map for activities in the current scope. */
    public fun activityInjectors(): SealantActivityInjectorsMap

    /** Returns the injector map for broadcast receivers in the current scope. */
    public fun broadcastReceiverInjectors(): SealantBroadcastReceiverInjectorsMap

    /** Returns the injector map for content providers in the current scope. */
    public fun contentProviderInjectors(): SealantContentProviderInjectorsMap

    /** Returns the injector map for services in the current scope. */
    public fun serviceInjectors(): SealantServiceInjectorsMap
}

/**
 * Contract implemented by Android entry points that want to use [injectViaSealant].
 *
 * The implementation selects the injector map appropriate for its base Android type.
 */
public interface SealantInjectable<T : Any> {

    /** Returns the injector map matching the current runtime category of [T]. */
    public fun getInjectorsMap(owner: SealantInjectorsOwner): Map<Class<out T>, SealantInjector<*>>
}
