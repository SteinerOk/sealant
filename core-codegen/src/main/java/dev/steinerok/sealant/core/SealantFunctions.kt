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
package dev.steinerok.sealant.core

import com.squareup.anvil.compiler.internal.reference.AnnotationReference
import com.squareup.anvil.compiler.internal.reference.AnvilCompilationExceptionAnnotationReference
import com.squareup.anvil.compiler.internal.reference.ClassReference
import com.squareup.anvil.compiler.internal.reference.argumentAt
import com.squareup.kotlinpoet.ClassName

/**
 *
 */
public fun buildVmScopeClassName(origScopeClassName: ClassName): ClassName =
    ClassName(origScopeClassName.packageName, "ViewModel_${origScopeClassName.simpleName}")

/**
 *
 */
public sealed class SealantFeature(public val name: String, public val index: Int) {

    public object Appcomponent : SealantFeature(name = "addAppcomponentSupport", index = 0)

    public object ViewModel : SealantFeature(name = "addViewModelSupport", index = 1)

    public object Fragment : SealantFeature(name = "addFragmentSupport", index = 2)

    public object Work : SealantFeature(name = "addWorkSupport", index = 3)
}

/**
 *
 */
public fun AnnotationReference.getSealantFeatureState(feature: SealantFeature): Boolean {
    return argumentAt(name = feature.name, index = feature.index)
        ?.value()
        ?: throw AnvilCompilationExceptionAnnotationReference(
            message = "Couldn't find ${feature.name} for $fqName.",
            annotationReference = this
        )
}

/**
 *
 */
public fun ClassReference.hasSealantFeatureInParents(
    feature: SealantFeature
): Boolean {
    val scope = getScopeFromComponentOrSubcomponent()
    var parentScope = scope.parentSealantScope()
    while (parentScope != null) {
        if (parentScope.hasSealantFeatureForScope(feature)) {
            return true
        } else {
            parentScope = parentScope.parentSealantScope()
        }
    }
    return false
}

/**
 *
 */
public fun ClassReference.parentSealantScope(): ClassReference? {
    return annotations
        .firstOrNull { it.fqName == FqNames.sealantConfiguration }
        ?.argumentAt("parentScope", 4)
        ?.value()
}

/**
 *
 */
public fun ClassReference.isComponentOrSubcomponentWithSealantFeature(
    feature: SealantFeature
): Boolean = isComponentOrSubcomponent() &&
    getScopeFromComponentOrSubcomponent().hasSealantFeatureForScope(feature)

/**
 *
 */
public fun ClassReference.hasSealantFeatureForScope(feature: SealantFeature): Boolean {
    return annotations
        .firstOrNull { it.fqName == FqNames.sealantConfiguration }
        ?.getSealantFeatureState(feature)
        ?: false
}
