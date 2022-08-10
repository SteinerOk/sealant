package dev.steinerok.sealant.core

import com.squareup.anvil.annotations.ContributesBinding
import com.squareup.anvil.annotations.ContributesMultibinding
import com.squareup.anvil.annotations.ContributesSubcomponent
import com.squareup.anvil.annotations.ContributesTo
import com.squareup.anvil.annotations.MergeComponent
import com.squareup.anvil.annotations.MergeSubcomponent
import com.squareup.anvil.compiler.internal.fqName
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.asClassName
import dagger.Binds
import dagger.BindsInstance
import dagger.Component
import dagger.MembersInjector
import dagger.Module
import dagger.Provides
import dagger.Subcomponent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.internal.Factory
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap
import dagger.multibindings.IntoSet
import dagger.multibindings.Multibinds
import dagger.multibindings.StringKey
import org.jetbrains.kotlin.name.FqName
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Provider
import javax.inject.Qualifier

private const val componentPkg = "dev.steinerok.sealant.core"

/**
 *
 */
public object ClassNames {
    public val optIn: ClassName = ClassName("kotlin", "OptIn")

    public val singleIn: ClassName = ClassName("dev.steinerok.di.common", "SingleIn")

    public val experimentalSealantApi: ClassName = ClassName(
        "dev.steinerok.sealant.maintenance",
        "ExperimentalSealantApi"
    )
    public val internalSealantApi: ClassName = ClassName(
        "dev.steinerok.sealant.maintenance.internal",
        "InternalSealantApi"
    )

    public val named: ClassName = Named::class.asClassName()
    public val inject: ClassName = Inject::class.asClassName()

    public val module: ClassName = Module::class.asClassName()
    public val binds: ClassName = Binds::class.asClassName()
    public val bindsInstance: ClassName = BindsInstance::class.asClassName()
    public val classKey: ClassName = ClassKey::class.asClassName()
    public val stringKey: ClassName = StringKey::class.asClassName()
    public val intoMap: ClassName = IntoMap::class.asClassName()
    public val intoSet: ClassName = IntoSet::class.asClassName()
    public val multibinds: ClassName = Multibinds::class.asClassName()
    public val subcomponentFactory: ClassName = Subcomponent.Factory::class.asClassName()
    public val componentFactory: ClassName = Component.Factory::class.asClassName()
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

    public val androidBundle: ClassName = ClassName("android.os", "Bundle")
    public val androidContext: ClassName = ClassName("android.content", "Context")
    public val androidxActivity: ClassName = ClassName("androidx.activity", "ComponentActivity")
    public val androidxFragment: ClassName = ClassName("androidx.fragment.app", "Fragment")
    public val androidxFragmentFactory: ClassName =
        ClassName("androidx.fragment.app", "FragmentFactory")
    public val androidxViewModel: ClassName = ClassName("androidx.lifecycle", "ViewModel")
    public val ssHandle: ClassName = ClassName("androidx.lifecycle", "SavedStateHandle")
    public val androidxListenableWorker: ClassName = ClassName("androidx.work", "ListenableWorker")
}

/**
 *
 */
public object FqNames {
    public val singleIn: FqName = FqName("org.steinerok.di.common.SingleIn")

    public val sealantConfiguration: FqName = FqName("$componentPkg.SealantConfiguration")

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

    public val bundle: FqName = FqName("android.os.Bundle")
    public val context: FqName = FqName("android.content.Context")
    public val androidxActivity: FqName = FqName("androidx.activity.ComponentActivity")
    public val androidxFragment: FqName = FqName("androidx.fragment.app.Fragment")
    public val androidxViewModel: FqName = FqName("androidx.lifecycle.ViewModel")
    public val androidxListenableWorker: FqName = FqName("androidx.work.ListenableWorker")
}

/**
 *
 */
public object MemberNames {
    public val bundleOf: MemberName = MemberName("androidx.core.os", "bundleOf")
}
