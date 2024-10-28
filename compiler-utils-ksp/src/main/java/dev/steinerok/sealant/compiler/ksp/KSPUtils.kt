package dev.steinerok.sealant.compiler.ksp

import com.google.devtools.ksp.getAllSuperTypes
import com.google.devtools.ksp.isDefault
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSName
import com.google.devtools.ksp.symbol.KSTypeAlias
import com.google.devtools.ksp.symbol.KSValueArgument
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.ksp.toClassName
import dev.steinerok.sealant.compiler.util.capitalized
import dev.steinerok.sealant.compiler.util.requireQualifiedName
import kotlin.reflect.KClass

/**
 * Return `dev.sealant.Test` into `DevSealantTest`.
 */
public val KSClassDeclaration.safeClassName: String
    get() = requireQualifiedName()
        .asString()
        .split(".")
        .joinToString(separator = "") { it.capitalized() }

/**
 *
 */
public fun KSDeclaration.requireQualifiedName(): KSName {
    return requireNotNull(qualifiedName) { "Qualified name was null for $this" }
}

/**
 *
 */
public fun KSDeclaration.requireContainingFile(): KSFile {
    return requireNotNull(containingFile) { "Containing file was null for $this" }
}


/**
 *
 */
public fun Resolver.getSymbolsWithAnnotation(annotation: KClass<*>): Sequence<KSAnnotated> {
    return getSymbolsWithAnnotation(annotation.requireQualifiedName())
}

/**
 *
 */
public fun Resolver.getSymbolsWithAnnotation(annotation: ClassName): Sequence<KSAnnotated> {
    return getSymbolsWithAnnotation(annotation.canonicalName)
}


/**
 *
 */
public fun KSAnnotated.requireAnnotation(annotation: KClass<out Annotation>): KSAnnotation {
    return requireAnnotation(annotation.asClassName())
}

/**
 *
 */
public fun KSAnnotated.requireAnnotation(annotation: ClassName): KSAnnotation {
    return findAnnotations(annotation).single()
}

/**
 *
 */
public fun KSAnnotated.isAnnotationPresent(annotation: KClass<out Annotation>): Boolean {
    return isAnnotationPresent(annotation.asClassName())
}

/**
 *
 */
public fun KSAnnotated.isAnnotationPresent(annotation: ClassName): Boolean {
    return findAnnotations(annotation).firstOrNull() != null
}

/**
 *
 */
public fun KSAnnotated.findAnnotations(annotation: KClass<out Annotation>): Sequence<KSAnnotation> {
    return findAnnotations(annotation.asClassName())
}

/**
 *
 */
public fun KSAnnotated.findAnnotations(annotation: ClassName): Sequence<KSAnnotation> {
    return annotations.filter { it.isAnnotation(annotation) }
}

/**
 *
 */
public fun KSAnnotation.isAnnotation(annotation: ClassName): Boolean {
    // we can skip resolving if the short name doesn't match
    if (shortName.asString() != annotation.simpleName) return false
    val declaration = annotationType.resolve().declaration
    return declaration.packageName.asString() == annotation.packageName
}


/**
 * Return whether, or not this [KSClassDeclaration] has a supertype of type [className] anywhere
 * it its supertype hierarchy
 */
public fun KSClassDeclaration.implements(className: ClassName): Boolean {
    return getAllSuperTypes().any { ksType ->
        //
        if (ksType.isError) return@any false
        //
        return@any when (val decl = ksType.declaration) {
            is KSClassDeclaration -> decl.toClassName()
            is KSTypeAlias -> decl.toClassName()
            else -> null
        }?.copy(nullable = ksType.isMarkedNullable) == className
    }
}


/**
 *
 */
public inline fun <reified T> KSAnnotation.argumentOfTypeAt(name: String): T {
    return requireNotNull(argumentOfTypeAtOrNull<T>(name)) {
        "Required argument with name '$name' of type '${T::class.qualifiedName} in '$shortName' but was null"
    }
}

/**
 *
 */
public inline fun <reified T> KSAnnotation.argumentOfTypeAtOrNull(name: String): T? {
    return argumentOfTypeWithMapperAtOrNull<T, T>(name) { _, value -> value }
}

/**
 *
 */
public inline fun <reified T, R> KSAnnotation.argumentOfTypeWithMapperAtOrNull(
    name: String,
    mapper: (arg: KSValueArgument, value: T) -> R,
): R? {
    return argumentAtOrNull(name)?.let { arg ->
        val value = arg.value
        check(value is T) {
            "Expected argument '$name' of type '${T::class.qualifiedName} but was '${arg.javaClass.name}'."
        }
        (value as T)?.let { mapper(arg, it) }
    }
}

/**
 *
 */
public fun KSAnnotation.argumentAtOrNull(name: String): KSValueArgument? {
    return arguments.find { it.name?.asString() == name }?.takeUnless { it.isDefault() }
}
