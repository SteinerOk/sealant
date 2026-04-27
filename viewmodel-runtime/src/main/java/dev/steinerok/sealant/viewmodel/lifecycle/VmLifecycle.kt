package dev.steinerok.sealant.viewmodel.lifecycle

import androidx.annotation.MainThread

/**
 * Lifecycle-like contract for objects retained for the lifetime of a ViewModel.
 *
 * It is intentionally minimal and currently exposes only an `onCleared` signal.
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

    /** Listener invoked when the retained lifecycle is cleared. */
    public fun interface OnClearedListener {
        /** Called when the owning retained object is being cleared permanently. */
        public fun onCleared()
    }
}

/** Retained lifecycle implementation specifically associated with a single ViewModel instance. */
public interface ViewModelLifecycle : RetainedLifecycle
