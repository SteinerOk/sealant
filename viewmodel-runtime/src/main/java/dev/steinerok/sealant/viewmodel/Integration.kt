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

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import dev.steinerok.sealant.core.internal.InternalSealantApi
import dev.steinerok.sealant.viewmodel.lifecycle.ViewModelLifecycle
import kotlin.reflect.KClass

/**
 * Marker scope used for generated ViewModel-only subcomponents.
 *
 * Bindings contributed with `@ContributesToViewModel` are ultimately installed into a generated
 * scope derived from this marker.
 */
public abstract class SealantViewModelScope private constructor()

/**
 * Base contract for the generated subcomponent responsible for creating Sealant ViewModels.
 *
 * Every enabled scope gets its own implementation that accepts a [SavedStateHandle] and
 * [ViewModelLifecycle] at creation time.
 */
public interface SealantViewModelSubcomponent {

    /** Factory that creates a scope-specific ViewModel subcomponent instance. */
    public interface Factory {

        /**
         * Creates a new ViewModel subcomponent for a single ViewModel creation request.
         */
        public fun create(
            ssHandle: SavedStateHandle,
            vmLifecycle: ViewModelLifecycle,
        ): SealantViewModelSubcomponent
    }

    /**
     * Parent-scope contract exposing ViewModel creation infrastructure to Android entry points.
     */
    public interface Parent {

        /** Set of ViewModel classes supported by the current parent scope. */
        @InternalSealantApi
        @get:SealantViewModelSupport.KeySet
        public val vmKeySet: Set<KClass<out ViewModel>>

        /** Map from scope class to the generated subcomponent factory for that scope. */
        @InternalSealantApi
        @get:SealantViewModelSupport.SubcomponentMap
        public val vmSubcomponentFactoryMap: Map<KClass<out Any>, () -> Factory>

        /** Entry point used by UI layers to build [androidx.lifecycle.ViewModelProvider.Factory] instances. */
        @InternalSealantApi
        public val vmFactoryCreator: SealantViewModelFactoryCreator
    }
}

/**
 * Internal view of a generated ViewModel subcomponent after it has been created.
 *
 * It exposes both the regular provider map and the assisted-factory map used by
 * [SealantViewModelFactory].
 */
@InternalSealantApi
public interface ViewModelFactoriesOwner {

    /** Map of directly instantiable ViewModels. */
    @get:SealantViewModelMap
    public val vmProviderMap: Map<KClass<out ViewModel>, () -> ViewModel>

    /** Map of assisted factories for ViewModels that require runtime arguments. */
    @get:SealantViewModelAssistedMap
    public val vmAssistedMap: Map<KClass<out ViewModel>, Any>
}
