package dev.steinerok.selant.sample.core.di

/**
 * A class that provides and maintains a single instance of a [AppComponent].
 *
 * NOTE: This should **only** be applied to the Application class.
 */
public interface AppComponentProvider {

    /**
     * An instance of the [AppComponent].
     */
    public val appComponent: AppComponent
}
