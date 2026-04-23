package dev.steinerok.di.common

/**
 * A class that provides and maintains a single instance of a [T].
 *
 * NOTE: This should **only** be applied to the Application class.
 */
public interface AppComponentProvider<T> {

    /**
     * An instance of the [T].
     */
    public val appComponent: T
}
