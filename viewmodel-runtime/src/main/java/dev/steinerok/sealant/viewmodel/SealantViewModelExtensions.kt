package dev.steinerok.sealant.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.MutableCreationExtras
import dagger.assisted.AssistedInject

/**
 * Returns a new `CreationExtras` with the original entries plus the passed in creation
 * callback. The callback is used by Sealant to create [AssistedInject]-annotated
 * [ContributesViewModel]s.
 *
 * @param callback A creation callback that takes an assisted factory and returns a `ViewModel`.
 */
public inline fun <reified VMF> CreationExtras.withCreationCallback(
    noinline callback: (VMF) -> ViewModel,
): CreationExtras = MutableCreationExtras(this).addCreationCallback(callback)

/**
 * Returns the `MutableCreationExtras` with the passed in creation callback added. The
 * callback is used by Sealant to create [AssistedInject]-annotated [ContributesViewModel]s.
 *
 * @param callback A creation callback that takes an assisted factory and returns a `ViewModel`.
 */
public inline fun <reified VMF> MutableCreationExtras.addCreationCallback(
    noinline callback: (VMF) -> ViewModel,
): CreationExtras = apply {
    this[SealantViewModelFactory.CREATION_CALLBACK_KEY] = { factory -> callback(factory as VMF) }
}
