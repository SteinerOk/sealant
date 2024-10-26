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
@file:Suppress("FunctionName")

package dev.steinerok.sealant.compiler

import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FileSpec.Companion.builder
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import kotlin.reflect.KClass

/**
 *
 */
public fun FileSpec(
    packageName: String,
    fileName: String,
    block: FileSpec.Builder.() -> Unit,
): FileSpec = builder(packageName, fileName).apply(block).build()

/**
 *
 */
public fun FileSpec(
    className: ClassName,
    block: FileSpec.Builder.() -> Unit,
): FileSpec = builder(className).apply(block).build()


/**
 *
 */
public fun AnnotationSpec(
    name: ClassName,
    block: AnnotationSpec.Builder.() -> Unit = {},
): AnnotationSpec = AnnotationSpec.builder(name).apply(block).build()

/**
 *
 */
public fun AnnotationSpec(
    type: KClass<out Annotation>,
    block: AnnotationSpec.Builder.() -> Unit = {},
): AnnotationSpec = AnnotationSpec.builder(type).apply(block).build()


/**
 *
 */
public fun ClassSpec(
    className: ClassName,
    block: TypeSpec.Builder.() -> Unit = {},
): TypeSpec = TypeSpec.classBuilder(className).apply(block).build()

/**
 *
 */
public fun ClassSpec(
    name: String,
    block: TypeSpec.Builder.() -> Unit = {},
): TypeSpec = TypeSpec.classBuilder(name).apply(block).build()


/**
 *
 */
public fun ObjectSpec(
    className: ClassName,
    block: TypeSpec.Builder.() -> Unit = {},
): TypeSpec = TypeSpec.objectBuilder(className).apply(block).build()

/**
 *
 */
public fun ObjectSpec(
    name: String,
    block: TypeSpec.Builder.() -> Unit = {},
): TypeSpec = TypeSpec.objectBuilder(name).apply(block).build()


/**
 *
 */
public fun InterfaceSpec(
    className: ClassName,
    block: TypeSpec.Builder.() -> Unit = {},
): TypeSpec = TypeSpec.interfaceBuilder(className).apply(block).build()

/**
 *
 */
public fun InterfaceSpec(
    name: String,
    block: TypeSpec.Builder.() -> Unit = {},
): TypeSpec = TypeSpec.interfaceBuilder(name).apply(block).build()


/**
 *
 */
public fun FunInterfaceSpec(
    className: ClassName,
    block: TypeSpec.Builder.() -> Unit = {},
): TypeSpec = TypeSpec.funInterfaceBuilder(className).apply(block).build()

/**
 *
 */
public fun FunInterfaceSpec(
    name: String,
    block: TypeSpec.Builder.() -> Unit = {},
): TypeSpec = TypeSpec.funInterfaceBuilder(name).apply(block).build()


/**
 *
 */
public fun CompanionObjectSpec(
    name: String? = null,
    block: TypeSpec.Builder.() -> Unit = {},
): TypeSpec = TypeSpec.companionObjectBuilder(name).apply(block).build()


/**
 *
 */
public fun PropertySpec(
    name: String,
    type: TypeName,
    vararg modifiers: KModifier,
    block: PropertySpec.Builder.() -> Unit = {},
): PropertySpec = PropertySpec.builder(name, type, *modifiers).apply(block).build()

/**
 *
 */
public fun PropertySpec(
    name: String,
    type: KClass<*>,
    vararg modifiers: KModifier,
    block: PropertySpec.Builder.() -> Unit = {},
): PropertySpec = PropertySpec.builder(name, type, *modifiers).apply(block).build()


/**
 *
 */
public fun ParameterSpec(
    name: String,
    type: TypeName,
    vararg modifiers: KModifier,
    block: ParameterSpec.Builder.() -> Unit = {},
): ParameterSpec = ParameterSpec.builder(name, type, *modifiers).apply(block).build()

/**
 *
 */
public fun ParameterSpec(
    name: String,
    type: KClass<*>,
    vararg modifiers: KModifier,
    block: ParameterSpec.Builder.() -> Unit = {},
): ParameterSpec = ParameterSpec.builder(name, type, *modifiers).apply(block).build()


/**
 *
 */
public fun ConstructorSpec(
    block: FunSpec.Builder.() -> Unit = {},
): FunSpec = FunSpec.constructorBuilder().apply(block).build()

/**
 *
 */
public fun FunSpec(
    name: String,
    block: FunSpec.Builder.() -> Unit = {},
): FunSpec = FunSpec.builder(name).apply(block).build()
