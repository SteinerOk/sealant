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
package dev.steinerok.sealant.compiler.embedded

import com.squareup.kotlinpoet.FileSpec
import dev.steinerok.sealant.compiler.AnnotationSpec
import dev.steinerok.sealant.compiler.ClassNames
import com.squareup.anvil.compiler.internal.buildFile as buildAnvilFileContent

/**
 *
 */
@Suppress("FunctionName")
public fun SealantFileSpecContent(
    packageName: String,
    fileName: String,
    block: FileSpec.Builder.() -> Unit,
): String = FileSpec.buildAnvilFileContent(
    packageName = packageName,
    fileName = fileName,
    generatorComment = "Generated by Sealant.\nhttps://github.com/SteinerOk/sealant",
    block = {
        //
        addSealantOptIns()
        //
        block()
    }
)

private fun FileSpec.Builder.addSealantOptIns() {
    addAnnotation(
        AnnotationSpec(ClassNames.optIn) {
            addMember("%T::class", ClassNames.internalSealantApi)
            addMember("%T::class", ClassNames.experimentalSealantApi)
        }
    )
}