package dev.steinerok.sealant.core

import kotlin.reflect.KClass

/**
 *
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
public annotation class SealantConfiguration(

    /**  */
    val addAppcomponentSupport: Boolean,

    /**  */
    val addViewModelSupport: Boolean,

    /**  */
    val addFragmentSupport: Boolean,

    /**  */
    val addWorkSupport: Boolean,

    /**  */
    val parentScope: KClass<*> = Any::class
)
