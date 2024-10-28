package dev.steinerok.sealant.compiler.ksp

import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import dev.steinerok.sealant.compiler.ClassNames
import dev.steinerok.sealant.compiler.SealantFeature

/**
 *
 */
public fun KSType.hasSealantFeatureForScope(feature: SealantFeature): Boolean {
    return declaration.hasSealantFeatureForScope(feature)
}

/**
 *
 */
public fun KSAnnotated.hasSealantFeatureForScope(feature: SealantFeature): Boolean {
    return findAnnotations(ClassNames.sealantConfiguration).firstOrNull()
        ?.hasSealantFeature(feature) ?: false
}

/**
 *
 */
public fun KSAnnotation.hasSealantFeature(feature: SealantFeature): Boolean {
    return argumentOfTypeAt<Boolean>(feature.name)
}

/**
 *
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

