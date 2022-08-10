package dev.steinerok.di.common

/**
 * Runtime utility method for performing a casting in code.
 */
public inline fun <reified T> Any.cast(): T = this as T
