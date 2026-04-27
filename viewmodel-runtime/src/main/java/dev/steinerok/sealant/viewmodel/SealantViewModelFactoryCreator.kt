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

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.SavedStateViewModelFactory
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.savedstate.SavedStateRegistryOwner
import dev.steinerok.sealant.core.internal.InternalSealantApi
import javax.inject.Inject
import javax.inject.Provider

/**
 * Builds [ViewModelProvider.Factory] instances backed by Sealant's generated ViewModel graph.
 *
 * This plays the same role as Hilt's internal factory creator: it combines the scope-level
 * registry of supported ViewModels with the generated subcomponent factories and produces a
 * factory that can be installed as the default one for an activity or fragment.
 */
public open class SealantViewModelFactoryCreator @InternalSealantApi @Inject constructor(
    private val application: Application,
    @param:SealantViewModelSupport.KeySet private val vmKeySet: @JvmSuppressWildcards Set<Class<out ViewModel>>,
    @param:SealantViewModelSupport.SubcomponentMap private val vmSubcomponentFactoryMap: Map<String, @JvmSuppressWildcards Provider<SealantViewModelSubcomponent.Factory>>,
) {

    /** Creates a Sealant-aware [ViewModelProvider.Factory] for the given [activity]. */
    public fun fromActivity(
        activity: ComponentActivity,
        delegateFactory: ViewModelProvider.Factory? = activity.defaultViewModelProviderFactory,
    ): ViewModelProvider.Factory = fromSsrOwner(
        owner = activity,
        defaultArgs = activity.intent?.extras,
        delegateFactory = delegateFactory,
    )

    /** Creates a Sealant-aware [ViewModelProvider.Factory] for the given [fragment]. */
    public fun fromFragment(
        fragment: Fragment,
        delegateFactory: ViewModelProvider.Factory? = fragment.defaultViewModelProviderFactory,
    ): ViewModelProvider.Factory = fromSsrOwner(
        owner = fragment,
        defaultArgs = fragment.arguments,
        delegateFactory = delegateFactory,
    )

    /**
     * Creates a Sealant-aware [ViewModelProvider.Factory] for an arbitrary
     * [SavedStateRegistryOwner].
     *
     * If [delegateFactory] is omitted, a default [SavedStateViewModelFactory] is used for
     * non-Sealant ViewModels.
     */
    public fun fromSsrOwner(
        owner: SavedStateRegistryOwner,
        defaultArgs: Bundle? = null,
        delegateFactory: ViewModelProvider.Factory? = null,
    ): ViewModelProvider.Factory {
        val verifiedDelegateFactory = delegateFactory
            ?: SavedStateViewModelFactory(application, owner, defaultArgs)
        return SealantViewModelFactory(
            vmKeySet = vmKeySet,
            delegateFactory = verifiedDelegateFactory,
            vmSubcomponentFactoryMap = vmSubcomponentFactoryMap,
        )
    }

    /** Exposes [SealantViewModelFactoryCreator] from a generated scope owner. */
    public interface Owner {

        /** Returns the creator configured for the current scope. */
        public fun sealantViewModelFactoryCreator(): SealantViewModelFactoryCreator
    }
}
