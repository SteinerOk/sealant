package dev.steinerok.sealant.sample.feature.home

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dev.steinerok.sealant.work.ContributesWorker
import dev.steinerok.selant.sample.core.di.AppScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import kotlin.time.Duration.Companion.seconds

/**
 *
 */
@ContributesWorker(AppScope::class)
public class FeatureHomeWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val appLevelFeatureHomeTool: AppLevelFeatureHomeTool,
    private val regularFeatureHomeTool: RegularFeatureHomeTool,
) : CoroutineWorker(appContext, workerParams) {

    init {
        Timber.i("Create FeatureHomeWorker at: ${System.currentTimeMillis()}")
    }

    override suspend fun doWork(): Result {
        Timber.i("Start work at: ${System.currentTimeMillis()} with $appLevelFeatureHomeTool and $regularFeatureHomeTool")
        withContext(Dispatchers.IO) { delay(3.seconds) }
        Timber.i("End work at: ${System.currentTimeMillis()}")
        return Result.success()
    }
}

/**
 *
 */
@OptIn(DelicateCoroutinesApi::class)
public fun launchWorks(appContext: Context): Any = GlobalScope.launch {
    WorkManager.getInstance(appContext)
        .enqueue(OneTimeWorkRequestBuilder<FeatureHomeWorker>().build())

    WorkManager.getInstance(appContext)
        .enqueue(OneTimeWorkRequestBuilder<FeatureHomeWorker>().build())

    withContext(Dispatchers.IO) { delay(3.seconds) }

    WorkManager.getInstance(appContext)
        .enqueue(OneTimeWorkRequestBuilder<FeatureHomeWorker>().build())
}
