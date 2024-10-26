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
package dev.steinerok.sealant.compiler.embedded

import com.squareup.anvil.annotations.ContributesBinding
import com.squareup.anvil.annotations.ContributesMultibinding
import com.squareup.anvil.annotations.ContributesSubcomponent
import com.squareup.anvil.annotations.ContributesTo
import com.squareup.anvil.annotations.MergeComponent
import com.squareup.anvil.annotations.MergeSubcomponent
import com.squareup.anvil.compiler.internal.fqName
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import org.jetbrains.kotlin.name.FqName
import javax.inject.Inject
import javax.inject.Qualifier

/**
 *
 */
public object FqNames {
    public val singleIn: FqName = FqName("com.squareup.anvil.annotations.optional.SingleIn")
    public val forScope: FqName = FqName("com.squareup.anvil.annotations.optional.ForScope")

    public val sealantConfiguration: FqName = FqName(
        "dev.steinerok.sealant.core.SealantConfiguration"
    )
    public val sealantIntegration: FqName = FqName(
        "dev.steinerok.sealant.core.SealantIntegration"
    )

    public val jvmSuppressWildcards: FqName = JvmSuppressWildcards::class.fqName

    public val inject: FqName = Inject::class.fqName
    public val qualifier: FqName = Qualifier::class.fqName

    public val assisted: FqName = Assisted::class.fqName
    public val assistedInject: FqName = AssistedInject::class.fqName
    public val assistedFactory: FqName = AssistedFactory::class.fqName

    public val contributesTo: FqName = ContributesTo::class.fqName
    public val contributesBinding: FqName = ContributesBinding::class.fqName
    public val contributesMultibinding: FqName = ContributesMultibinding::class.fqName

    public val mergeComponent: FqName = MergeComponent::class.fqName
    public val mergeSubcomponent: FqName = MergeSubcomponent::class.fqName
    public val contributesSubcomponent: FqName = ContributesSubcomponent::class.fqName
}