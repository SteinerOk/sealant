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
package dev.steinerok.sealant.compiler

import com.squareup.anvil.annotations.ContributesBinding
import com.squareup.anvil.annotations.ContributesMultibinding
import com.squareup.anvil.annotations.ContributesSubcomponent
import com.squareup.anvil.annotations.ContributesTo
import com.squareup.anvil.annotations.MergeComponent
import com.squareup.anvil.annotations.MergeSubcomponent
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.asClassName
import dagger.Binds
import dagger.BindsInstance
import dagger.MembersInjector
import dagger.Module
import dagger.Provides
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.internal.Factory
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap
import dagger.multibindings.IntoSet
import dagger.multibindings.Multibinds
import dagger.multibindings.StringKey
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Provider
import javax.inject.Qualifier
import kotlin.reflect.KClass

/**
 *
 */
public object ClassNames {
    public val optIn: ClassName = ClassName("kotlin", "OptIn")

    public val singleIn: ClassName = ClassName(
        "com.squareup.anvil.annotations.optional", "SingleIn"
    )
    public val forScope: ClassName = ClassName(
        "com.squareup.anvil.annotations.optional", "ForScope"
    )

    public val sealantConfiguration: ClassName = ClassName(
        "dev.steinerok.sealant.core", "SealantConfiguration"
    )
    public val sealantIntegration: ClassName = ClassName(
        "dev.steinerok.sealant.core", "SealantIntegration"
    )

    public val experimentalSealantApi: ClassName = ClassName(
        "dev.steinerok.sealant.core", "ExperimentalSealantApi"
    )
    public val internalSealantApi: ClassName = ClassName(
        "dev.steinerok.sealant.core.internal", "InternalSealantApi"
    )

    public val named: ClassName = Named::class.asClassName()
    public val inject: ClassName = Inject::class.asClassName()
    public val qualifier: ClassName = Qualifier::class.asClassName()

    public val module: ClassName = Module::class.asClassName()
    public val binds: ClassName = Binds::class.asClassName()
    public val bindsInstance: ClassName = BindsInstance::class.asClassName()
    public val classKey: ClassName = ClassKey::class.asClassName()
    public val stringKey: ClassName = StringKey::class.asClassName()
    public val intoMap: ClassName = IntoMap::class.asClassName()
    public val intoSet: ClassName = IntoSet::class.asClassName()
    public val multibinds: ClassName = Multibinds::class.asClassName()
    public val subcomponentFactory: ClassName = MergeSubcomponent.Factory::class.asClassName()
    public val componentFactory: ClassName = MergeComponent.Factory::class.asClassName()
    public val provides: ClassName = Provides::class.asClassName()
    public val daggerFactory: ClassName = Factory::class.asClassName()
    public val provider: ClassName = Provider::class.asClassName()
    public val membersInjector: ClassName = MembersInjector::class.asClassName()
    public val assisted: ClassName = Assisted::class.asClassName()
    public val assistedInject: ClassName = AssistedInject::class.asClassName()
    public val assistedFactory: ClassName = AssistedFactory::class.asClassName()

    public val contributesTo: ClassName = ContributesTo::class.asClassName()
    public val contributesBinding: ClassName = ContributesBinding::class.asClassName()
    public val contributesMultibinding: ClassName = ContributesMultibinding::class.asClassName()
    public val mergeComponent: ClassName = MergeComponent::class.asClassName()
    public val mergeSubcomponent: ClassName = MergeSubcomponent::class.asClassName()
    public val contributesSubcomponent: ClassName = ContributesSubcomponent::class.asClassName()

    public val javaClazz: ClassName = Class::class.asClassName()
    public val kotlinClazz: ClassName = KClass::class.asClassName()

    public val androidBundle: ClassName = ClassName("android.os", "Bundle")
    public val androidContext: ClassName = ClassName("android.content", "Context")
    public val androidxActivity: ClassName = ClassName("androidx.activity", "ComponentActivity")
    public val androidxFragment: ClassName = ClassName("androidx.fragment.app", "Fragment")
    public val androidxFragmentFactory: ClassName = ClassName(
        "androidx.fragment.app", "FragmentFactory"
    )
    public val androidxViewModel: ClassName = ClassName("androidx.lifecycle", "ViewModel")
    public val ssHandle: ClassName = ClassName("androidx.lifecycle", "SavedStateHandle")
    public val androidxListenableWorker: ClassName = ClassName("androidx.work", "ListenableWorker")
}

/**
 *
 */
public object MemberNames {
    public val bundleOf: MemberName = MemberName("androidx.core.os", "bundleOf")
}
