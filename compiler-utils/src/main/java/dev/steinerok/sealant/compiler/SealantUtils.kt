/*
 * Copyright 2024 Ihor Kushnirenko
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
package dev.steinerok.sealant.compiler

import com.squareup.kotlinpoet.ClassName

/**
 *
 */
public sealed class SealantFeature(public val name: String, public val index: Int) {

    public data object Appcomponent : SealantFeature(name = "addAppcomponentSupport", index = 0)

    public data object ViewModel : SealantFeature(name = "addViewModelSupport", index = 1)

    public data object Fragment : SealantFeature(name = "addFragmentSupport", index = 2)

    public data object Work : SealantFeature(name = "addWorkSupport", index = 3)
}

/**
 *
 */
public fun buildVmScopeClassName(origScopeClassName: ClassName): ClassName =
    ClassName(origScopeClassName.packageName, "ViewModel_${origScopeClassName.simpleName}")
