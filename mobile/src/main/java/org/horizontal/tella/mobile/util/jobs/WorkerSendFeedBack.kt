package org.horizontal.tella.mobile.util.jobs

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.rx2.await
import kotlinx.coroutines.withContext
import org.hzontal.tella.keys.key.LifecycleMainKey
import org.horizontal.tella.mobile.MyApplication
import org.horizontal.tella.mobile.data.database.DataSource
import org.horizontal.tella.mobile.di.WorkerDependenciesEntryPoint
import org.horizontal.tella.mobile.domain.exception.NoConnectivityException
import org.horizontal.tella.mobile.util.LockTimeoutManager

class WorkerSendFeedBack(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        var holdsTimeout = false
        try {
            // Obtain dependencies from the Hilt application graph via entry point.
            val appContext = applicationContext
            val entryPoint = EntryPointAccessors.fromApplication(
                appContext,
                WorkerDependenciesEntryPoint::class.java
            )
            val feedbackRepository = entryPoint.feedbackRepository()
            val statusProvider = entryPoint.statusProvider()

            if (!statusProvider.isOnline()) {
                return@withContext Result.retry()
            }

            val mainKey = try {
                MyApplication.getMainKeyHolder().get().key.encoded
            } catch (e: LifecycleMainKey.MainKeyUnavailableException) {
                return@withContext Result.retry()
            }

            val dataSource = DataSource.getInstance(appContext, mainKey)

            val feedbackInstances = dataSource.listFeedBackInstances()?.await()

            if (feedbackInstances.isNullOrEmpty()) {
                return@withContext Result.success()
            }

            withContext(Dispatchers.Main.immediate) {
                LockTimeoutManager.onBackgroundWorkStarted()
                holdsTimeout = true
            }

            for (instance in feedbackInstances) {
                try {
                    feedbackRepository.submitFeedbackInstance(instance).await()
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (e: Exception) {
                    // If this is a connectivity issue, ask WorkManager to retry later.
                    if (e is NoConnectivityException) {
                        return@withContext Result.retry()
                    }
                    // For non-connectivity errors, do not put the whole worker into an infinite
                    // retry loop. The repository already marks the instance as SUBMISSION_ERROR
                    // so we can simply continue with the next one.
                }
            }

            Result.success()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (t: Throwable) {
            Result.retry()
        } finally {
            if (holdsTimeout) {
                withContext(NonCancellable + Dispatchers.Main.immediate) {
                    LockTimeoutManager.onBackgroundWorkFinished()
                }
            }
        }
    }
}
