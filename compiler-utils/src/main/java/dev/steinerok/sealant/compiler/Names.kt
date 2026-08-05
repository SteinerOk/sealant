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
    public val kotlinClazz: ClassName = KClass::class.asClassName()
    public val any: ClassName = Any::class.asClassName()
    public val nothing: ClassName = Nothing::class.asClassName()
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

    // JSR-330 style core annotations, provided natively by Metro.
    public val inject: ClassName = ClassName("dev.zacsweers.metro", "Inject")
    public val named: ClassName = ClassName("dev.zacsweers.metro", "Named")
    public val qualifier: ClassName = ClassName("dev.zacsweers.metro", "Qualifier")

    public val binds: ClassName = ClassName("dev.zacsweers.metro", "Binds")
    public val bindingContainer: ClassName = ClassName("dev.zacsweers.metro", "BindingContainer")
    public val provides: ClassName = ClassName("dev.zacsweers.metro", "Provides")
    public val assisted: ClassName = ClassName("dev.zacsweers.metro", "Assisted")
    public val assistedFactory: ClassName = ClassName("dev.zacsweers.metro", "AssistedFactory")
    public val assistedInject: ClassName = ClassName("dev.zacsweers.metro", "AssistedInject")
    public val intoMap: ClassName = ClassName("dev.zacsweers.metro", "IntoMap")
    public val intoSet: ClassName = ClassName("dev.zacsweers.metro", "IntoSet")
    public val multibinds: ClassName = ClassName("dev.zacsweers.metro", "Multibinds")
    public val stringKey: ClassName = ClassName("dev.zacsweers.metro", "StringKey")
    public val membersInjector: ClassName = ClassName("dev.zacsweers.metro", "MembersInjector")

    public val contributesTo: ClassName = ClassName("dev.zacsweers.metro", "ContributesTo")
    public val singleIn: ClassName = ClassName("dev.zacsweers.metro", "SingleIn")
    public val graphExtension: ClassName = ClassName("dev.zacsweers.metro", "GraphExtension")
    public val graphExtensionFactory: ClassName = graphExtension.nestedClass("Factory")

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
