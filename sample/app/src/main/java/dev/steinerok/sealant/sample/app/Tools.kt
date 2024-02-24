package dev.steinerok.sealant.sample.app

import com.squareup.anvil.annotations.ContributesBinding
import com.squareup.anvil.annotations.ContributesTo
import com.squareup.anvil.annotations.optional.SingleIn
import dev.steinerok.selant.sample.core.di.AccountScope
import dev.steinerok.selant.sample.core.di.AppScope
import dev.steinerok.selant.sample.core.di.GuestScope
import timber.log.Timber
import javax.inject.Inject

/**
 *
 */
class RegularTool @Inject constructor() {
    init {
        Timber.d("Create RegularTool at: ${System.currentTimeMillis()}")
    }
}

/**
 *
 */
@ContributesTo(scope = AppScope::class)
@ContributesTo(scope = GuestScope::class)
@ContributesTo(scope = AccountScope::class)
interface RegularToolProvider {
    fun regularTool(): RegularTool
}


/**
 *
 */
interface AppLevelTool

/**
 *
 */
@ContributesBinding(scope = AppScope::class)
@SingleIn(AppScope::class)
class RealAppLevelTool @Inject constructor() : AppLevelTool {
    init {
        Timber.d("Create RealAppLevelTool at: ${System.currentTimeMillis()}")
    }
}

/**
 *
 */
@ContributesTo(scope = AppScope::class)
interface AppLevelToolProvider {
    fun appLevelTool(): AppLevelTool
}


/**
 *
 */
interface GuestLevelTool

/**
 *
 */
@ContributesBinding(scope = GuestScope::class)
@SingleIn(GuestScope::class)
class RealGuestLevelTool @Inject constructor() : GuestLevelTool {
    init {
        Timber.d("Create RealGuestLevelTool at: ${System.currentTimeMillis()}")
    }
}

/**
 *
 */
@ContributesTo(scope = GuestScope::class)
interface GuestLevelToolProvider {
    fun guestLevelTool(): GuestLevelTool
}
