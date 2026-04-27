package dev.steinerok.di.common

/**
 * Exposes the application-level root graph to code that only has access to an `Application`.
 *
 * Sealant samples use this as a lightweight bridge from Android framework entry points to the
 * app component. In practice this interface is expected to be implemented by the `Application`.
 */
public interface AppComponentProvider<T> {

    /**
     * Root application graph instance.
     */
    public val appComponent: T
}
