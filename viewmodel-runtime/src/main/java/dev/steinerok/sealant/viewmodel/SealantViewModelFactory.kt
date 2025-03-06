/*
 * Copyright 2025 Ihor Kushnirenko
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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import dev.steinerok.sealant.core.internal.InternalSealantApi
import javax.inject.Provider

/**
 *
 */
public class SealantViewModelFactory internal constructor(
    private val vmKeySet: Set<Class<out ViewModel>>,
    private val delegateFactory: ViewModelProvider.Factory,
    private val vmSubcomponentFactoryMap: Map<String, Provider<SealantViewModelSubcomponent.Factory>>,
) : ViewModelProvider.Factory {

    private val primaryFactory by lazy(LazyThreadSafetyMode.NONE) {
        SealantSavedStateViewModelFactory()
    }

    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        return if (vmKeySet.contains(modelClass)) {
            primaryFactory.create(modelClass, extras)
        } else {
            delegateFactory.create(modelClass, extras)
        }
    }

    private inner class SealantSavedStateViewModelFactory : ViewModelProvider.Factory {

        @OptIn(InternalSealantApi::class)
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
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
            }.get().create(ssHandle = extras.createSavedStateHandle()) as ViewModelFactoriesOwner
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
            delegateFactory: ViewModelProvider.Factory
        ): ViewModelProvider.Factory = SealantViewModelFactory(
            vmKeySet = parent.vmKeySet,
            delegateFactory = delegateFactory,
            vmSubcomponentFactoryMap = parent.vmSubcomponentFactoryMap
        )
    }
}