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
import dev.steinerok.sealant.core.internal.InternalSealantApi
import kotlin.reflect.KClass

private typealias FragmentProviderMap = Map<KClass<out Fragment>, () -> Fragment>

/**
 * A [FragmentFactory] backed by Sealant's fragment multibinding map.
 *
 * If the requested fragment class is present in the generated provider map, the fragment is
 * created through Metro. Otherwise this factory falls back to the default
 * [FragmentFactory.instantiate] behavior.
 */
public class SealantFragmentFactory @InternalSealantApi constructor(
    private val fragProviderMap: FragmentProviderMap,
) : FragmentFactory() {

    override fun instantiate(classLoader: ClassLoader, className: String): Fragment {
        val fragmentClazz = loadFragmentClass(classLoader, className)

        return fragProviderMap[fragmentClazz.kotlin]?.invoke() ?: super.instantiate(classLoader, className)
    }

    /** Exposes a configured [SealantFragmentFactory] from the owning component. */
    public interface Owner {

        /** Returns the fragment factory associated with the current scope. */
        public fun sealantFragmentFactory(): SealantFragmentFactory
    }
}
