package dev.steinerok.sealant.sample.app

import android.app.Application
import androidx.work.Configuration
import com.squareup.anvil.annotations.ContributesTo
import dev.steinerok.di.common.cast
import dev.steinerok.sealant.work.SealantWorkerFactory
import dev.steinerok.selant.sample.core.di.AppComponent
import dev.steinerok.selant.sample.core.di.AppComponentProvider
import dev.steinerok.selant.sample.core.di.AppScope
import timber.log.Timber
import javax.inject.Inject
import androidx.work.Configuration.Provider as WorkManagerConfigurationProvider

/**
 *
 */
class SealantSampleApp : Application(), AppComponentProvider, WorkManagerConfigurationProvider {

    private val skeletonComponent: SkeletonComponent by lazy(LazyThreadSafetyMode.NONE) {
        SkeletonComponent.create(this)
    }

    override val appComponent: AppComponent by lazy(LazyThreadSafetyMode.NONE) {
        skeletonComponent.cast<AppComponent.Parent>().appComponent()
    }

    @Inject
    lateinit var diWorkerFactory: SealantWorkerFactory

    override val workManagerConfiguration: Configuration
        get() {
            injectIfNecessary()
            return Configuration.Builder().setWorkerFactory(diWorkerFactory).build()
        }

    override fun onCreate() {
        super.onCreate()

        injectIfNecessary()

        if (BuildConfig.DEBUG) {
            Timber.plant(SimpleSealantDebugTree())
        }
    }

    private fun injectIfNecessary() {
        if (!::diWorkerFactory.isInitialized) {
            appComponent.cast<Injector>().inject(this)
        }
    }

    @ContributesTo(scope = AppScope::class)
    interface Injector {
        fun inject(instance: SealantSampleApp)
    }
}

private class SimpleSealantDebugTree : Timber.DebugTree() {
    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        super.log(priority, "X-Log: $tag", message, t)
    }
}
