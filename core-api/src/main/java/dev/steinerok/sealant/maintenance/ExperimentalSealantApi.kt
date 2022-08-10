package dev.steinerok.sealant.maintenance

/**
 * Indicates that the annotated Sealant API is experimental and subject to change.
 */
@RequiresOptIn(
    message = "These APIs are experimental. They might still contain bugs or be changed in the future.",
    level = RequiresOptIn.Level.WARNING
)
public annotation class ExperimentalSealantApi
