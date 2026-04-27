package dev.steinerok.sealant.viewmodel.lifecycle

import androidx.annotation.MainThread

/**
 * A class for registered listeners on a retained lifecycle (generally backed up by a ViewModel).
 */
public interface RetainedLifecycle {

    /**
     * Adds a new [OnClearedListener] for receiving a callback when the lifecycle is cleared.
     *
     * @param listener The listener that should be added.
     */
    @MainThread
    public fun addOnClearedListener(listener: OnClearedListener)

    /**
     * Removes a [OnClearedListener] previously added via [addOnClearedListener].
     *
     * @param listener The listener that should be removed.
     */
    @MainThread
    public fun removeOnClearedListener(listener: OnClearedListener)

    /** Listener for when the retained lifecycle is cleared. */
    public fun interface OnClearedListener {
        public fun onCleared()
    }
}

/**
 * A class for registering listeners on the ViewModel lifecycle.
 */
public interface ViewModelLifecycle : RetainedLifecycle
