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
package dev.steinerok.sealant.fragment

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentFactory
import dev.steinerok.sealant.maintenance.internal.InternalSealantApi
import javax.inject.Provider

private typealias FragmentProviderMap = Map<Class<out Fragment>, @JvmSuppressWildcards Provider<Fragment>>

/**
 * A [FragmentFactory] that can hold onto multiple other FragmentFactory [Provider]'s.
 *
 * Note this was designed to be used with [FragmentKey].
 */
public class SealantFragmentFactory @InternalSealantApi constructor(
    private val fragProviderMap: FragmentProviderMap,
) : FragmentFactory() {

    override fun instantiate(classLoader: ClassLoader, className: String): Fragment {
        val fragmentClazz = loadFragmentClass(classLoader, className)
        return fragProviderMap[fragmentClazz]?.get() ?: super.instantiate(classLoader, className)
    }

    /**
     *
     */
    public interface Owner {

        /**  */
        public fun sealantFragmentFactory(): SealantFragmentFactory
    }
}
