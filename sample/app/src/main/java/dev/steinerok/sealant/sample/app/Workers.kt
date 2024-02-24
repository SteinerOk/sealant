package dev.steinerok.sealant.sample.app

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
class MainWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val appLevelTool: AppLevelTool,
    private val regularTool: RegularTool,
) : CoroutineWorker(appContext, workerParams) {

    init {
        Timber.i("Create MainWorker at: ${System.currentTimeMillis()}")
    }

    override suspend fun doWork(): Result {
        Timber.i("Start work at: ${System.currentTimeMillis()} with $appLevelTool and $regularTool")
        withContext(Dispatchers.IO) { delay(3.seconds) }
        Timber.i("End work at: ${System.currentTimeMillis()}")
        return Result.success()
    }
}

/**
 *
 */
@OptIn(DelicateCoroutinesApi::class)
fun launchWorks(appContext: Context) = GlobalScope.launch {
    WorkManager.getInstance(appContext)
        .enqueue(OneTimeWorkRequestBuilder<MainWorker>().build())

    WorkManager.getInstance(appContext)
        .enqueue(OneTimeWorkRequestBuilder<MainWorker>().build())

    withContext(Dispatchers.IO) { delay(3.seconds) }

    WorkManager.getInstance(appContext)
        .enqueue(OneTimeWorkRequestBuilder<MainWorker>().build())
}
