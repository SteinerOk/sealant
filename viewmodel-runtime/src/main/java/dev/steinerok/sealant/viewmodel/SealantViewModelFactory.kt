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
import dev.steinerok.sealant.viewmodel.lifecycle.RetainedLifecycleImpl
import javax.inject.Provider

/**
 * [ViewModelProvider.Factory] implementation that understands Sealant-generated ViewModel graphs.
 *
 * For registered ViewModel classes it spins up a generated subcomponent, binds a
 * [androidx.lifecycle.SavedStateHandle] and [dev.steinerok.sealant.viewmodel.lifecycle.ViewModelLifecycle],
 * then resolves either a direct provider or an assisted factory. Unknown ViewModels are delegated
 * to [delegateFactory].
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
        return if (modelClass in vmKeySet) {
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

            val lifecycle = RetainedLifecycleImpl()

            val wmfOwner = requireNotNull(vmSubcomponentFactoryMap[scopeClass.name]) {
                "Expected the Sealant Subcomponent factory class '${scopeClass.name}' to be " +
                        "available in the multi-binding of @SealantViewModelSupport.SubcomponentMap " +
                        "but none was found. Found only: ${vmSubcomponentFactoryMap.keys.toList()}"
            }.get().create(
                ssHandle = extras.createSavedStateHandle(),
                vmLifecycle = lifecycle,
            ) as ViewModelFactoriesOwner

            val provider = wmfOwner.vmProviderMap[modelClass]
            val creationCallback = extras[CREATION_CALLBACK_KEY]
            val assistedFactory = wmfOwner.vmAssistedMap[modelClass]

            @Suppress("UNCHECKED_CAST")
            val viewModel = when {
                provider != null && assistedFactory != null -> {
                    throw AssertionError(
                        "Found the @ContributesViewModel-annotated class ${modelClass.name} in both the " +
                                "multi-bindings of @SealantViewModelMap and @SealantViewModelAssistedMap."
                    )
                }

                assistedFactory != null -> {
                    checkNotNull(creationCallback) {
                        "Found @ContributesViewModel-annotated class ${modelClass.name} using " +
                                "@AssistedInject but no creation callback was provided in CreationExtras."
                    }
                    creationCallback.invoke(assistedFactory) as T
                }

                provider != null -> {
                    check(creationCallback == null) {
                        "Found creation callback but class ${modelClass.name} " +
                                "does not have an assisted factory specified in @ContributesViewModel."
                    }
                    provider.get() as T
                }

                else -> {
                    throw IllegalStateException(
                        "Expected the @ContributesViewModel-annotated class ${modelClass.name} " +
                                "to be available in the multi-binding of @SealantViewModelMap but none was found."
                    )
                }
            }

            viewModel.addCloseable { lifecycle.dispatchOnCleared() }

            return viewModel
        }
    }

    public companion object {

        /** Creation extra key for the callbacks that create @AssistedInject-annotated ViewModels. */
        @JvmField
        public val CREATION_CALLBACK_KEY: CreationExtras.Key<(Any) -> ViewModel> =
            object : CreationExtras.Key<(Any) -> ViewModel> {}

        /**
         * Creates a factory backed by the generated ViewModel infrastructure exposed from [parent].
         */
        @OptIn(InternalSealantApi::class)
        @JvmStatic
        public fun createInternal(
            parent: SealantViewModelSubcomponent.Parent,
            delegateFactory: ViewModelProvider.Factory,
        ): ViewModelProvider.Factory = SealantViewModelFactory(
            vmKeySet = parent.vmKeySet,
            delegateFactory = delegateFactory,
            vmSubcomponentFactoryMap = parent.vmSubcomponentFactoryMap,
        )
    }
}
