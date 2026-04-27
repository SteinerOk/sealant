package dev.steinerok.sealant.compiler.ksp

import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import dev.steinerok.sealant.compiler.ClassNames
import dev.steinerok.sealant.compiler.SealantFeature

/** Returns `true` when the resolved type declaration enables the requested Sealant [feature]. */
public fun KSType.hasSealantFeatureForScope(feature: SealantFeature): Boolean {
    return declaration.hasSealantFeatureForScope(feature)
}

/** Returns `true` when the annotated declaration enables the requested Sealant [feature]. */
public fun KSAnnotated.hasSealantFeatureForScope(feature: SealantFeature): Boolean {
    return findAnnotations(ClassNames.sealantConfiguration).firstOrNull()
        ?.hasSealantFeature(feature) ?: false
}

/** Reads a feature flag from a `@SealantConfiguration` annotation instance. */
public fun KSAnnotation.hasSealantFeature(feature: SealantFeature): Boolean {
    return argumentOfTypeAt<Boolean>(feature.name)
}

/**
 * Walks up the configured parent-scope chain until it finds a scope that enables [feature].
 */
public fun KSClassDeclaration.parentScopeWithSealantFeature(
    feature: SealantFeature,
): KSClassDeclaration? {
    var parentScope = parentScope()
    while (parentScope != null) {
        if (parentScope.hasSealantFeatureForScope(feature)) {
            return parentScope
        } else {
            parentScope = if (parentScope.isAnnotationPresent(ClassNames.sealantConfiguration)) {
                parentScope.parentScope()
            } else null
        }
    }
    return null
}
