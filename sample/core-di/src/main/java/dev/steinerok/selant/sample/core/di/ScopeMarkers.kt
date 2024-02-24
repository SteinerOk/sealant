package dev.steinerok.selant.sample.core.di

import dev.steinerok.sealant.core.SealantConfiguration

/**
 *
 */
public abstract class SkeletonScope private constructor()

/**
 *
 */
@SealantConfiguration(
    addAppcomponentSupport = true,
    addViewModelSupport = true,
    addFragmentSupport = true,
    addWorkSupport = true,
    parentScope = SkeletonScope::class
)
public abstract class AppScope private constructor()

/**
 *
 */
@SealantConfiguration(
    addAppcomponentSupport = false,
    addViewModelSupport = true,
    addFragmentSupport = true,
    addWorkSupport = false,
    parentScope = AppScope::class
)
public abstract class GuestScope private constructor()

/**
 *
 */
@SealantConfiguration(
    addAppcomponentSupport = false,
    addViewModelSupport = false,
    addFragmentSupport = false,
    addWorkSupport = false,
    parentScope = AppScope::class
)
public abstract class AccountScope private constructor()
