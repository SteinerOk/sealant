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
package dev.steinerok.sealant.maintenance.internal

/**
 * Indicates that the annotated Sealant API is internal.
 * Do not depend on this API in your own client code.
 */
@RequiresOptIn(
    message = "This is internal API for the Sealant libraries. Do not depend on this API in your own client code.",
    level = RequiresOptIn.Level.ERROR
)
public annotation class InternalSealantApi
