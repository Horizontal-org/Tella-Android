package org.horizontal.tella.mobile.util.jobs

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.Dispatchers
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

            // If listFeedBackInstances() returns an Rx Single, we safely call blockingGet() on IO.
            // If you later expose a suspend version, just replace this line.
            val feedbackInstances = dataSource.listFeedBackInstances()?.blockingGet()

            if (feedbackInstances.isNullOrEmpty()) {
                setNoTimeOut(false)
                return@withContext Result.success()
            } else {
                setNoTimeOut(true)
            }

            // Same note: if submitFeedbackInstance returns Single, keep blockingGet() on IO.
            for (instance in feedbackInstances) {
                try {
                    feedbackRepository.submitFeedbackInstance(instance).blockingGet()
                } catch (e: Exception) {
                    // If this is a connectivity issue, ask WorkManager to retry later.
                    if (e is NoConnectivityException) {
                        setNoTimeOut(false)
                        return@withContext Result.retry()
                    }
                    // For non-connectivity errors, do not put the whole worker into an infinite
                    // retry loop. The repository already marks the instance as SUBMISSION_ERROR
                    // so we can simply continue with the next one.
                }
            }

            setNoTimeOut(false)
            Result.success()
        } catch (t: Throwable) {
            setNoTimeOut(false)
            Result.retry()
        }
    }

    private fun setNoTimeOut(enableNoTimeout: Boolean) {
        MyApplication.getMainKeyHolder().timeout =
            if (enableNoTimeout) LifecycleMainKey.NO_TIMEOUT
            else LockTimeoutManager.IMMEDIATE_SHUTDOWN
    }
}
