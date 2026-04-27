package dev.steinerok.sealant.viewmodel.lifecycle

import android.os.Looper

/** Internal main-thread implementation of [ViewModelLifecycle]. */
internal class RetainedLifecycleImpl : ViewModelLifecycle {

    private val listeners = mutableSetOf<RetainedLifecycle.OnClearedListener>()
    private var onClearedDispatched = false

    override fun addOnClearedListener(listener: RetainedLifecycle.OnClearedListener) {
        ThreadUtil.ensureMainThread()
        throwIfOnClearedDispatched()
        listeners.add(listener)
    }

    override fun removeOnClearedListener(listener: RetainedLifecycle.OnClearedListener) {
        ThreadUtil.ensureMainThread()
        throwIfOnClearedDispatched()
        listeners.remove(listener)
    }

    fun dispatchOnCleared() {
        ThreadUtil.ensureMainThread()
        onClearedDispatched = true
        for (listener in listeners) {
            listener.onCleared()
        }
    }

    private fun throwIfOnClearedDispatched() {
        check(!onClearedDispatched) {
            "There was a race between the call to add/remove an OnClearedListener and onCleared(). " +
                    "This can happen when posting to the Main thread from a background thread, " +
                    "which is not supported."
        }
    }
}

/** Thread utility methods used by the retained lifecycle implementation. */
private object ThreadUtil {

    private var mainThread: Thread? = null

    /** Returns `true` when the current thread is Android's main thread. */
    fun isMainThread(): Boolean {
        if (mainThread == null) {
            mainThread = Looper.getMainLooper().thread
        }
        return Thread.currentThread() === mainThread
    }

    /** Verifies that the current thread is the main thread and throws otherwise. */
    fun ensureMainThread() {
        check(isMainThread()) { "Must be called on the Main thread." }
    }
}
