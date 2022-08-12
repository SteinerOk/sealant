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
package dev.steinerok.sealant.work

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import dev.steinerok.sealant.maintenance.internal.InternalSealantApi

/**
 * Creates a [ListenableWorker] using Dagger for the [SealantWorkerFactory].
 */
@InternalSealantApi
public fun interface WorkerAssistedFactory<T : ListenableWorker> {

    /**
     * Creates a [ListenableWorker] of type `T`.
     */
    public fun create(appContext: Context, workerParams: WorkerParameters): T
}
