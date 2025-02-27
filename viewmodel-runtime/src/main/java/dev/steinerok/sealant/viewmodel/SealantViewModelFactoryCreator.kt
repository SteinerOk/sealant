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
 * The same functionality as InternalFactoryFactory inside DefaultViewModelFactories in Hilt
 */
public class SealantViewModelFactoryCreator
@InternalSealantApi
@Inject
internal constructor(
    private val application: Application,
    @SealantViewModelMap.KeySet private val vmKeySet: @JvmSuppressWildcards Set<Class<out ViewModel>>,
    @SealantViewModelMap.SubcomponentMap private val vmSubcomponentFactoryMap: Map<String, @JvmSuppressWildcards Provider<SealantViewModelSubcomponent.Factory>>
) {

    public fun fromActivity(
        activity: ComponentActivity,
        delegateFactory: ViewModelProvider.Factory? = null
    ): ViewModelProvider.Factory = fromSsrOwner(activity, activity.intent?.extras, delegateFactory)

    public fun fromFragment(
        fragment: Fragment,
        delegateFactory: ViewModelProvider.Factory? = null
    ): ViewModelProvider.Factory = fromSsrOwner(fragment, fragment.arguments, delegateFactory)

    public fun fromSsrOwner(
        owner: SavedStateRegistryOwner,
        defaultArgs: Bundle? = null,
        delegateFactory: ViewModelProvider.Factory? = null
    ): ViewModelProvider.Factory {
        val verifiedDelegateFactory = delegateFactory
            ?: SavedStateViewModelFactory(application, owner, defaultArgs)
        return SealantViewModelFactory(
            owner = owner,
            defaultArgs = defaultArgs,
            vmKeySet = vmKeySet,
            delegateFactory = verifiedDelegateFactory,
            vmSubcomponentFactoryMap = vmSubcomponentFactoryMap
        )
    }

    /**
     *
     */
    public interface Owner {

        /**  */
        public fun sealantViewModelFactoryCreator(): SealantViewModelFactoryCreator
    }
}
