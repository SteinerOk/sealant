package dev.steinerok.di.common

import javax.inject.Qualifier

/**
 * Annotation for an Application Context dependency.
 */
@Qualifier
@Target(
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER,
    AnnotationTarget.VALUE_PARAMETER,
    AnnotationTarget.FIELD
)
public annotation class ApplicationContext
