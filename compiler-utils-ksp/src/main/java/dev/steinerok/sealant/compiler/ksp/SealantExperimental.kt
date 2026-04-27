package dev.steinerok.sealant.compiler.ksp

import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import dev.steinerok.sealant.compiler.ClassNames
import dev.steinerok.sealant.compiler.SealantFeature

/** Returns the required `scope` argument from the declaration or fails with a descriptive error. */
public fun KSClassDeclaration.scope(): KSType {
    return requireNotNull(scopeOrNull()) {
        "Couldn't find scope for $this."
    }
}

/**
 * Returns the effective `scope` argument from annotations that expose such a parameter.
 *
 * When multiple scope-bearing annotations are present, all of them must point to the same scope.
 */
public fun KSClassDeclaration.scopeOrNull(): KSType? {
    val annotationsWithScopeParameter = annotations
        .filter { it.hasScopeParameter() }
        .toList()
        .ifEmpty { return null }
    return scopeForAnnotationsWithScopeParameters(this, annotationsWithScopeParameter)
}

private fun KSAnnotation.hasScopeParameter(): Boolean {
    return (annotationType.resolve().declaration as? KSClassDeclaration)
        ?.primaryConstructor?.parameters?.firstOrNull()?.name?.asString() == "scope"
}

private fun scopeForAnnotationsWithScopeParameters(
    clazz: KSClassDeclaration,
    annotations: List<KSAnnotation>,
): KSType {
    val explicitScopes = annotations.map { annotation ->
        annotation.scopeParameter()
    }

    @Suppress("RETURN_VALUE_NOT_USED")
    explicitScopes.scan(
        initial = explicitScopes.first().declaration.requireQualifiedName().asString()
    ) { previous, next ->
        check(previous == next.declaration.requireQualifiedName().asString()) {
            "All scopes on annotations must be the same."
        }
        previous
    }

    return explicitScopes.first()
}

private fun KSAnnotation.scopeParameter(): KSType {
    return requireNotNull(scopeParameterOrNull()) {
        "Couldn't find a scope parameter."
    }
}

private fun KSAnnotation.scopeParameterOrNull(): KSType? {
    return arguments.firstOrNull { it.name?.asString() == "scope" }?.let { it.value as? KSType }
}

internal fun KSClassDeclaration.parentScope(): KSClassDeclaration? {
    return requireAnnotation(ClassNames.sealantConfiguration)
        .argumentOfTypeAtOrNull<KSType>("parentScope")
        ?.declaration as? KSClassDeclaration
}

/** Reads all scopes listed in `@SealantIntegration(scopes = ...)` on the current declaration. */
public fun KSAnnotated.findScopesForIntegration(): Sequence<KSClassDeclaration> {
    return findAnnotations(ClassNames.sealantIntegration)
        .flatMap { annotation ->
            annotation.argumentOfTypeAtOrNull<List<KSType>>("scopes")
                ?.mapNotNull { it.declaration as? KSClassDeclaration }
                .orEmpty()
        }
        .distinct()
}

/** Filters integration scopes down to those that enable the requested Sealant [feature]. */
public fun KSAnnotated.findScopesForSealantFeatureIntegration(
    feature: SealantFeature,
): Sequence<KSClassDeclaration> {
    return findScopesForIntegration().filter { it.hasSealantFeatureForScope(feature) }
}
