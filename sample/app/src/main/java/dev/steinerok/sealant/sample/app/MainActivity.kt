package dev.steinerok.sealant.sample.app

import android.app.Application
import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.fragment.app.commitNow
import androidx.fragment.app.viewModels
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import dagger.Module
import dagger.Provides
import com.squareup.anvil.annotations.optional.SingleIn
import dev.steinerok.di.common.cast
import dev.steinerok.sealant.appcomponent.InjectWith
import dev.steinerok.sealant.fragment.ContributesFragment
import dev.steinerok.sealant.fragment.SealantFragmentFactory
import dev.steinerok.sealant.viewmodel.ContributesToViewModel
import dev.steinerok.sealant.viewmodel.ContributesViewModel
import dev.steinerok.sealant.viewmodel.SealantViewModelFactoryCreator
import dev.steinerok.sealant.viewmodel.SealantViewModelScope
import dev.steinerok.selant.sample.core.di.AppComponentProvider
import dev.steinerok.selant.sample.core.di.AppScope
import dev.steinerok.selant.sample.core.di.GuestScope
import dev.steinerok.selant.sample.core.di.GuestSubcomponent
import timber.log.Timber
import javax.inject.Inject
import kotlin.random.Random

data class Info1(val time1: String)
data class Info2(val time2: String)

@Module
@ContributesToViewModel(AppScope::class)
object AppScopeVmSpecificModule {

    @Provides
    fun provideInfo1(handle: SavedStateHandle) = Info1(
        time1 = "${handle.get<String>("time_1")!!}_${Random.nextInt()}"
    )

    @Provides
    @SingleIn(SealantViewModelScope::class)
    fun provideInfo2(handle: SavedStateHandle) = Info2(
        time2 = handle.get<String>("time_2")!!
    )
}

@ContributesViewModel(AppScope::class)
class AppToAppViewModel @Inject constructor(
    private val application: Application,
    private val ssHandle: SavedStateHandle,
    private val appLevelTool: AppLevelTool,
    private val info1_1: Info1,
    private val info1_2: Info1,
) : ViewModel() {
    init {
        Timber.w("$info1_1 = $info1_2 -> ${ssHandle.keys().toList()}")
        Timber.w("$appLevelTool = none")
    }
}

@ContributesViewModel(AppScope::class)
class AppToApp2ViewModel @Inject constructor(
    private val application: Application,
    private val ssHandle: SavedStateHandle,
    private val appLevelTool: AppLevelTool,
) : ViewModel() {
    init {
        Timber.w("$appLevelTool = none2")
    }
}

@ContributesViewModel(GuestScope::class)
class GuestToGuestViewModel @Inject constructor(
    private val application: Application,
    private val ssHandle: SavedStateHandle,
    private val appLevelTool: AppLevelTool,
    private val guestLevelTool: GuestLevelTool,
) : ViewModel() {
    init {
        Timber.w("no_info -> ${ssHandle.keys().toList()}")
        Timber.w("$appLevelTool = appLevelTool")
        Timber.w("$guestLevelTool = guestLevelTool")
    }
}

/**
 *
 */
@InjectWith(AppScope::class)
class MainActivity : BaseActivity() {

    @Inject
    lateinit var baseAppLevelTool: AppLevelTool

    override fun onCreate(savedInstanceState: Bundle?) {
        //
        LocalAppInjection.inject(this)
        //
        supportFragmentManager.fragmentFactory = sFragmentFactory
        //
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        //
        launchWorks(applicationContext)
        //
        Timber.i("baseAppLevelTool: $baseAppLevelTool")

        if (savedInstanceState == null) {
            supportFragmentManager.commitNow {
                val args = bundleOf("time_1" to "AppTime: ${System.currentTimeMillis()}")
                add(R.id.content, MainChildFragment::class.java, args)
            }
        }
    }
}

@ContributesFragment(scope = AppScope::class)
class MainChildFragment @Inject constructor(
    vmfCreator: SealantViewModelFactoryCreator,
    private val appLevelTool: AppLevelTool,
) : BaseFragment(vmfCreator, R.layout.fragment_main_child) {

    private val viewModel: AppToAppViewModel by viewModels()

    init {
        Timber.w("Init -> $this")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val guestSub = requireContext().applicationContext.cast<AppComponentProvider>()
            .appComponent.cast<GuestSubcomponent.Parent>()
            .guestSubcomponentFactory()
            .create()

        childFragmentManager.fragmentFactory = guestSub.cast<SealantFragmentFactory.Owner>()
            .sealantFragmentFactory()

        super.onCreate(savedInstanceState)

        Timber.w("$this 0-> ${arguments?.keySet()?.toList()}")
        Timber.w("$this 1-> ${arguments?.getString("time_1")}")
        Timber.w("$this 2-> $appLevelTool")
        Timber.w("$this 3-> ${childFragmentManager.fragmentFactory}")
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        if (savedInstanceState == null) {
            childFragmentManager.commitNow {
                val args = bundleOf("time_2" to "AppTime: ${System.currentTimeMillis()}")
                add(R.id.content, MainChildChildFragment::class.java, args)
            }
        }
        Timber.w("$this 4-> $viewModel")
    }
}

@ContributesFragment(scope = GuestScope::class)
class MainChildChildFragment @Inject constructor(
    vmfCreator: SealantViewModelFactoryCreator,
    private val defFragmentFactory: SealantFragmentFactory,
) : BaseFragment(vmfCreator, R.layout.fragment_main_child_child) {

    private val viewModel1: AppToAppViewModel by viewModels(
        ownerProducer = { requireParentFragment() },
        extrasProducer = { requireParentFragment().defaultViewModelCreationExtras },
        factoryProducer = { requireParentFragment().defaultViewModelProviderFactory }
    )

    private val viewModel2: GuestToGuestViewModel by viewModels()
    private val viewModel3: AppToApp2ViewModel by viewModels()

    init {
        Timber.w("Init -> $this")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.w("$this 0-> ${arguments?.keySet()?.toList()}")
        Timber.w("$this 1-> ${arguments?.getString("time_2")}")
        Timber.w("$this 2-> $defFragmentFactory")
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Timber.w("$this 4-> $viewModel1")
        Timber.w("$this 5-> $viewModel2")
        Timber.w("$this 6-> $viewModel3")
    }
}
