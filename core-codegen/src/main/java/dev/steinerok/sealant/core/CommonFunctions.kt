package dev.steinerok.sealant.core

import com.squareup.anvil.compiler.internal.reference.ClassReference
import com.squareup.anvil.compiler.internal.reference.allSuperTypeClassReferences
import com.squareup.kotlinpoet.ClassName
import org.jetbrains.kotlin.name.FqName

/**
 *
 */
public fun ClassReference.isComponentOrSubcomponent(): Boolean {
    return isAnnotatedWith(FqNames.mergeComponent) || isAnnotatedWith(FqNames.mergeSubcomponent)
}

/**
 *
 */
public fun ClassReference.getScopeFromComponentOrSubcomponent(): ClassReference {
    return annotations.first { annotation ->
        annotation.fqName == FqNames.mergeComponent ||
            annotation.fqName == FqNames.mergeSubcomponent
    }.scope()
}

/**
 *
 */
public fun ClassReference.getScopeFrom(annotationFqName: FqName): ClassReference {
    return annotations.first { annotation -> annotation.fqName == annotationFqName }.scope()
}

/**
 *
 */
public fun ClassReference.isActivity(): Boolean = allSuperTypeClassReferences()
    .any { it.fqName == FqNames.androidxActivity }

/**
 *
 */
public fun ClassReference.isFragment(): Boolean = allSuperTypeClassReferences()
    .any { it.fqName == FqNames.androidxFragment }

/**
 *
 */
public fun ClassReference.isViewModel(): Boolean = allSuperTypeClassReferences()
    .any { it.fqName == FqNames.androidxViewModel }

/**
 *
 */
public fun ClassReference.isWorker(): Boolean = allSuperTypeClassReferences()
    .any { it.fqName == FqNames.androidxListenableWorker }

/**
 *
 */
public fun ClassName.generateSimpleNameString(
    separator: String = "_"
): String = simpleNames.joinToString(separator)
