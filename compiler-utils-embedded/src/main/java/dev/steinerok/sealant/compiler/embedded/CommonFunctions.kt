/*
 * Copyright 2022 Ihor Kushnirenko
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dev.steinerok.sealant.compiler.embedded

import com.squareup.anvil.compiler.internal.reference.ClassReference
import com.squareup.anvil.compiler.internal.reference.allSuperTypeClassReferences
import com.squareup.anvil.compiler.internal.reference.asClassName
import com.squareup.kotlinpoet.ClassName
import org.jetbrains.kotlin.name.FqName

/**
 *
 */
public fun ClassReference.implements(className: ClassName): Boolean {
    return allSuperTypeClassReferences().any { it.asClassName() == className }
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
