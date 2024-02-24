package dev.steinerok.selant.sample.core.di

import com.squareup.anvil.annotations.ContributesSubcomponent
import com.squareup.anvil.annotations.ContributesTo
import dagger.BindsInstance
import dagger.Component
import dagger.Module
import com.squareup.anvil.annotations.optional.SingleIn

/**
 * A top level [Component] for our dependency graph, this [Component] should be a [AppScope]
 * and only initialized once per app launch via the [AppComponentProvider]. All
 * [Module] or classes that contribute to [AppScope] will be available in the [AppComponent] graph.
 */
@ContributesSubcomponent(
    scope = AppScope::class,
    parentScope = SkeletonScope::class
)
@SingleIn(AppScope::class)
public interface AppComponent {

    @ContributesTo(SkeletonScope::class)
    public interface Parent {
        public fun appComponent(): AppComponent
    }
}

/**
 *
 */
@ContributesSubcomponent(
    scope = GuestScope::class,
    parentScope = AppScope::class
)
@SingleIn(GuestScope::class)
public interface GuestSubcomponent {

    /**
     * A [Component.Factory] that outlines how we want to create this [GuestSubcomponent].
     */
    @ContributesSubcomponent.Factory
    public interface Factory {
        public fun create(): GuestSubcomponent
    }

    @ContributesTo(AppScope::class)
    public interface Parent {
        public fun guestSubcomponentFactory(): Factory
    }
}

/**
 *
 */
@ContributesSubcomponent(
    scope = AccountScope::class,
    parentScope = AppScope::class
)
@SingleIn(AccountScope::class)
public interface AccountSubcomponent {

    @ContributesSubcomponent.Factory
    public interface Factory {
        public fun create(@BindsInstance instance: LoggedInAccount): AccountSubcomponent
    }

    @ContributesTo(AppScope::class)
    public interface Parent {
        public fun accountSubcomponentFactory(): Factory
    }
}

/**
 *
 */
public data class LoggedInAccount(
    val id: Long,
    val employeeId: Long,
    val enterpriseId: Long,
    val enterpriseDomain: String
)

/**
 *
 */
public interface LoggedInAccountProvider {
    /**  */
    public val loggedInAccount: LoggedInAccount
}