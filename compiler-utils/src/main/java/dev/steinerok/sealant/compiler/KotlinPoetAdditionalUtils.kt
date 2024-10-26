/*
 * Copyright 2024 Ihor Kushnirenko
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
package dev.steinerok.sealant.compiler

import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec

/**
 *
 */
public fun TypeSpec.Builder.addContributesToAnnotation(
    scopeClassName: ClassName,
    block: AnnotationSpec.Builder.() -> Unit = {},
): TypeSpec.Builder = addAnnotation(
    AnnotationSpec(ClassNames.contributesTo) {
        addMember("scope·=·%T::class", scopeClassName)
        block()
    }
)

/**
 *
 */
public fun TypeSpec.Builder.addPrimaryConstructorAndProperties(
    vararg propertySpecs: PropertySpec,
): TypeSpec.Builder {
    val properties = propertySpecs.map { pSpec ->
        pSpec.toBuilder().initializer(pSpec.name).build()
    }
    val parameters = properties.map { property ->
        ParameterSpec(property.name, property.type)
    }
    val constructor = ConstructorSpec {
        addParameters(parameters)
        addAnnotation(AnnotationSpec(ClassNames.inject))
    }
    return this
        .primaryConstructor(constructor)
        .addProperties(properties)
}

/**
 *
 */
public fun ClassName.generateSimpleNameString(
    separator: String = "_",
): String = simpleNames.joinToString(separator)
