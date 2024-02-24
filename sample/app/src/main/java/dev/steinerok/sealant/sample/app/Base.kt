package dev.steinerok.sealant.sample.app

import android.content.Context
import android.content.ContextWrapper
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.squareup.anvil.annotations.ContributesTo
import com.squareup.anvil.annotations.optional.SingleIn
import dev.steinerok.di.common.cast
import dev.steinerok.sealant.appcomponent.InjectorsResolver
import dev.steinerok.sealant.appcomponent.SealantInjectable
import dev.steinerok.sealant.appcomponent.SealantInjector
import dev.steinerok.sealant.appcomponent.SealantInjectorsOwner
import dev.steinerok.sealant.appcomponent.injectViaSealant
import dev.steinerok.sealant.fragment.SealantFragmentFactory
import dev.steinerok.sealant.viewmodel.SealantViewModelFactoryCreator
import dev.steinerok.selant.sample.core.di.AccountScope
import dev.steinerok.selant.sample.core.di.AppComponentProvider
import dev.steinerok.selant.sample.core.di.AppScope
import dev.steinerok.selant.sample.core.di.GuestScope
import dev.steinerok.selant.sample.core.di.LoggedInAccount
import dev.steinerok.selant.sample.core.di.LoggedInAccountProvider
import timber.log.Timber
import javax.inject.Inject

/**
 *
 */
abstract class BaseFragment @JvmOverloads constructor(
    private val vmfCreator: SealantViewModelFactoryCreator,
    @LayoutRes contentLayoutId: Int = ResourcesCompat.ID_NULL
) : Fragment(contentLayoutId) {

    override val defaultViewModelProviderFactory: ViewModelProvider.Factory
        get() {
            check(isAdded) { "Can't access ViewModels from detached fragment" }
            return vmfCreator.fromFragment(this, super.defaultViewModelProviderFactory)
        }
}

/**
 *
 */
abstract class BaseActivity : AppCompatActivity(), SealantInjectable<ComponentActivity> {

    @Inject
    lateinit var sFragmentFactory: SealantFragmentFactory

    @Inject
    lateinit var sVmfCreator: SealantViewModelFactoryCreator

    override val defaultViewModelProviderFactory: ViewModelProvider.Factory
        get() {
            checkNotNull(application) {
                "Your activity is not yet attached to the " +
                        "Application instance. You can't request ViewModel before onCreate call."
            }
            return sVmfCreator.fromActivity(this, super.defaultViewModelProviderFactory)
        }

    override fun getInjectorsMap(
        owner: SealantInjectorsOwner
    ): Map<Class<out ComponentActivity>, SealantInjector<*>> = owner.activityInjectors()
}


abstract class UnAuthedActivity : BaseActivity()

abstract class AuthedActivity : BaseActivity(), LoggedInAccountProvider {

    override fun onCreate(savedInstanceState: Bundle?) {
        //
        val accountManager = applicationContext.cast<AppComponentProvider>()
            .appComponent.cast<AccountManagerProvider>().getAccountManager()

        super.onCreate(savedInstanceState)
    }

    override val loggedInAccount: LoggedInAccount
        get() = TODO("Not yet implemented")
}

@SingleIn(AppScope::class)
class AccountManager @Inject constructor() {

}

@ContributesTo(scope = AppScope::class)
interface AccountManagerProvider {
    fun getAccountManager(): AccountManager
}

/**
 *
 */
object LocalAppInjection {

    fun inject(injectableComponent: SealantInjectable<*>) {
        val context = when (injectableComponent) {
            is ContextWrapper -> injectableComponent.applicationContext
            else -> throw IllegalArgumentException("Unsupported injectable: $injectableComponent")
        }
        inject(injectableComponent, context)
    }

    fun inject(injectableComponent: SealantInjectable<*>, context: Context) {
        val appContext = context.applicationContext
        val injectorsResolver: InjectorsResolver = { scope, injectable ->
            when (scope) {
                AppScope::class -> {
                    appContext.cast<AppComponentProvider>().appComponent.cast()
                }

                GuestScope::class -> {
                    Timber.i("$injectable -> GuestScope")
                    TODO()
                }

                AccountScope::class -> {
                    if (injectable is LoggedInAccountProvider) {
                        Timber.i("$injectable -> AccountScope")
                        TODO()
                    } else throw IllegalArgumentException(
                        "Failed requirement LoggedInUserProvider with scope: $scope for injectable: $injectable"
                    )
                }

                else -> throw IllegalArgumentException(
                    "Unknown injection scope: $scope for injectable: $injectable"
                )
            }
        }
        injectViaSealant(injectableComponent, injectorsResolver)
    }
}
