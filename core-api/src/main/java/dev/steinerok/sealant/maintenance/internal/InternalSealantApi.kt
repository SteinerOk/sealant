package dev.steinerok.sealant.maintenance.internal

/**
 * Indicates that the annotated Sealant API is internal.
 * Do not depend on this API in your own client code.
 */
@RequiresOptIn(
    message = "This is internal API for the Sealant libraries. Do not depend on this API in your own client code.",
    level = RequiresOptIn.Level.ERROR
)
public annotation class InternalSealantApi
