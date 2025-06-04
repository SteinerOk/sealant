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
@file:Suppress("DEPRECATION")

package dev.steinerok.sealant.viewmodel

import android.os.Bundle
import androidx.lifecycle.AbstractSavedStateViewModelFactory
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.savedstate.SavedStateRegistryOwner
import dev.steinerok.sealant.core.internal.InternalSealantApi
import javax.inject.Provider

/**
 *
 */
@Deprecated("Use SealantViewModelFactory instead.")
public class LegacySealantViewModelFactory internal constructor(
    owner: SavedStateRegistryOwner,
    defaultArgs: Bundle?,
    private val vmKeySet: Set<Class<out ViewModel>>,
    private val delegateFactory: ViewModelProvider.Factory,
    private val vmSubcomponentFactoryMap: Map<String, Provider<SealantViewModelSubcomponent.Factory>>
) : ViewModelProvider.Factory {

    private val primaryFactory by lazy(LazyThreadSafetyMode.NONE) {
        SealantSavedStateViewModelFactory(owner, defaultArgs)
    }

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return if (vmKeySet.contains(modelClass)) {
            primaryFactory.create(modelClass)
        } else {
            delegateFactory.create(modelClass)
        }
    }

    private inner class SealantSavedStateViewModelFactory(
        owner: SavedStateRegistryOwner,
        defaultArgs: Bundle?
    ) : AbstractSavedStateViewModelFactory(owner, defaultArgs) {

        @OptIn(InternalSealantApi::class)
        override fun <T : ViewModel> create(
            key: String,
            modelClass: Class<T>,
            handle: SavedStateHandle
        ): T {
            val scopeClass = requireNotNull(
                modelClass.getAnnotation(ContributesViewModel::class.java)?.scope?.java
            ) {
                "Expected the @ContributesViewModel-annotated class '${modelClass.name}' " +
                        "but required annotation was not found."
            }
            val wmfOwner = requireNotNull(vmSubcomponentFactoryMap[scopeClass.name]) {
                "Expected the Sealant Subcomponent factory class '${scopeClass.name}' to be " +
                        "available in the multi-binding of @SealantViewModelMap.SubcomponentMap " +
                        "but none was found. Found only: ${vmSubcomponentFactoryMap.keys.toList()}"
            }.get().create(handle) as ViewModelFactoriesOwner
            @Suppress("UNCHECKED_CAST")
            return requireNotNull(wmfOwner.vmProviderMap[modelClass]) {
                "Expected the @ContributesViewModel-annotated class '${modelClass.name}' to be " +
                        "available in the multi-binding of @SealantViewModelMap but " +
                        "none was found. Found only: ${wmfOwner.vmProviderMap.keys.toList()}"
            }.get() as T
        }
    }

    public companion object {

        @OptIn(InternalSealantApi::class)
        @JvmStatic
        public fun createInternal(
            parent: SealantViewModelSubcomponent.Parent,
            owner: SavedStateRegistryOwner,
            defaultArgs: Bundle?,
            delegateFactory: ViewModelProvider.Factory
        ): ViewModelProvider.Factory = LegacySealantViewModelFactory(
            owner = owner,
            defaultArgs = defaultArgs,
            vmKeySet = parent.vmKeySet,
            delegateFactory = delegateFactory,
            vmSubcomponentFactoryMap = parent.vmSubcomponentFactoryMap
        )
    }
}
