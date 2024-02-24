package dev.steinerok.sealant.sample.feature.home

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
public class RegularFeatureHomeTool @Inject constructor() {
    init {
        Timber.d("Create RegularFeatureHomeTool at: ${System.currentTimeMillis()}")
    }
}

/**
 *
 */
@ContributesTo(scope = AppScope::class)
@ContributesTo(scope = GuestScope::class)
@ContributesTo(scope = AccountScope::class)
public interface RegularFeatureHomeToolProvider {
    public fun regularToolFeatureHome(): RegularFeatureHomeTool
}


/**
 *
 */
public interface AppLevelFeatureHomeTool

/**
 *
 */
@ContributesBinding(scope = AppScope::class)
@SingleIn(AppScope::class)
public class RealAppLevelFeatureHomeTool @Inject constructor() : AppLevelFeatureHomeTool {
    init {
        Timber.d("Create RealAppLevelFeatureHomeTool at: ${System.currentTimeMillis()}")
    }
}

/**
 *
 */
@ContributesTo(scope = AppScope::class)
public interface AppLevelFeatureHomeToolProvider {
    public fun appLevelToolFeatureHome(): AppLevelFeatureHomeTool
}
