package dev.steinerok.sealant.sample.app

import android.app.Application
import android.content.Context
import com.squareup.anvil.annotations.ContributesTo
import com.squareup.anvil.annotations.MergeComponent
import dagger.BindsInstance
import dagger.Component
import dagger.Module
import dagger.Provides
import dev.steinerok.di.common.ApplicationContext
import com.squareup.anvil.annotations.optional.SingleIn
import dev.steinerok.selant.sample.core.di.SkeletonScope

/**
 *
 */
@MergeComponent(SkeletonScope::class)
@SingleIn(SkeletonScope::class)
interface SkeletonComponent {

    /**
     * A [Component.Factory] that outlines how we want to create this [SkeletonComponent].
     */
    @Component.Factory
    interface Factory {

        /**
         * Creates an instance of a [SkeletonComponent].
         *
         * @param instance A [Application] object that will be bound to our Dagger graph via
         * [BindsInstance] for easy injection.
         */
        fun create(@BindsInstance instance: Application): SkeletonComponent
    }

    companion object {
        fun create(app: Application): SkeletonComponent =
            DaggerSkeletonComponent.factory().create(app)
    }
}

/**
 *
 */
@ContributesTo(SkeletonScope::class)
@Module
class ApplicationContextModule {

    /**  */
    @SingleIn(SkeletonScope::class)
    @Provides
    @ApplicationContext
    fun provideApplicationContext(app: Application): Context = app.applicationContext
}
