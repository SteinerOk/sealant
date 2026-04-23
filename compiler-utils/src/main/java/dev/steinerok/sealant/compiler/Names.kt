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

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.asClassName
import kotlin.reflect.KClass

/**
 *
 */
public object ClassNames {
    public val javaClazz: ClassName = Class::class.asClassName()
    public val kotlinClazz: ClassName = KClass::class.asClassName()
    public val optIn: ClassName = ClassName("kotlin", "OptIn")

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

    public val inject: ClassName = ClassName("javax.inject", "Inject")
    public val named: ClassName = ClassName("javax.inject", "Named")
    public val provider: ClassName = ClassName("javax.inject", "Provider")
    public val qualifier: ClassName = ClassName("javax.inject", "Qualifier")

    public val binds: ClassName = ClassName("dagger", "Binds")
    public val bindsInstance: ClassName = ClassName("dagger", "BindsInstance")
    public val module: ClassName = ClassName("dagger", "Module")
    public val provides: ClassName = ClassName("dagger", "Provides")
    public val assisted: ClassName = ClassName("dagger.assisted", "Assisted")
    public val assistedFactory: ClassName = ClassName("dagger.assisted", "AssistedFactory")
    public val assistedInject: ClassName = ClassName("dagger.assisted", "AssistedInject")
    public val classKey: ClassName = ClassName("dagger.multibindings", "ClassKey")
    public val intoMap: ClassName = ClassName("dagger.multibindings", "IntoMap")
    public val intoSet: ClassName = ClassName("dagger.multibindings", "IntoSet")
    public val multibinds: ClassName = ClassName("dagger.multibindings", "Multibinds")
    public val stringKey: ClassName = ClassName("dagger.multibindings", "StringKey")
    public val membersInjector: ClassName = ClassName("dagger", "MembersInjector")
    public val daggerFactory: ClassName = ClassName("dagger.internal", "Factory")

    public val contributesBinding: ClassName =
        ClassName("com.squareup.anvil.annotations", "ContributesBinding")
    public val contributesMultibinding: ClassName =
        ClassName("com.squareup.anvil.annotations", "ContributesMultibinding")
    public val contributesSubcomponent: ClassName =
        ClassName("com.squareup.anvil.annotations", "ContributesSubcomponent")
    public val contributesSubcomponentFactory: ClassName =
        ClassName("com.squareup.anvil.annotations", "ContributesSubcomponent", "Factory")
    public val contributesTo: ClassName =
        ClassName("com.squareup.anvil.annotations", "ContributesTo")
    public val mergeComponent: ClassName =
        ClassName("com.squareup.anvil.annotations", "MergeComponent")
    public val mergeComponentFactory: ClassName =
        ClassName("com.squareup.anvil.annotations", "MergeComponent", "Factory")
    public val mergeSubcomponent: ClassName =
        ClassName("com.squareup.anvil.annotations", "MergeSubcomponent")
    public val mergeSubcomponentFactory: ClassName =
        ClassName("com.squareup.anvil.annotations", "MergeSubcomponent", "Factory")

    public val singleIn: ClassName =
        ClassName("com.squareup.anvil.annotations.optional", "SingleIn")
    public val forScope: ClassName =
        ClassName("com.squareup.anvil.annotations.optional", "ForScope")

    public val androidContext: ClassName = ClassName("android.content", "Context")
    public val androidApplication: ClassName = ClassName("android.app", "Application")
    public val androidBundle: ClassName = ClassName("android.os", "Bundle")
    public val androidActivity: ClassName = ClassName("android.app", "Activity")
    public val androidBroadcastReceiver: ClassName =
        ClassName("android.content", "BroadcastReceiver")
    public val androidContentProvider: ClassName = ClassName("android.content", "ContentProvider")
    public val androidService: ClassName = ClassName("android.app", "Service")
    public val androidxFragment: ClassName = ClassName("androidx.fragment.app", "Fragment")
    public val androidxFragmentFactory: ClassName =
        ClassName("androidx.fragment.app", "FragmentFactory")
    public val androidxViewModel: ClassName = ClassName("androidx.lifecycle", "ViewModel")
    public val androidxSsHandle: ClassName = ClassName("androidx.lifecycle", "SavedStateHandle")
    public val androidxListenableWorker: ClassName = ClassName("androidx.work", "ListenableWorker")
}

/**
 *
 */
public object MemberNames {
    public val bundleOf: MemberName = MemberName("androidx.core.os", "bundleOf")
}
