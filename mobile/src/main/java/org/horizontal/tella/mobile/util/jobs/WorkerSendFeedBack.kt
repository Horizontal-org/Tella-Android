package org.horizontal.tella.mobile.util.jobs

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.hzontal.tella.keys.key.LifecycleMainKey
import org.horizontal.tella.mobile.MyApplication
import org.horizontal.tella.mobile.data.database.DataSource
import org.horizontal.tella.mobile.domain.repository.feedback.FeedBackRepository
import org.horizontal.tella.mobile.util.LockTimeoutManager
import org.horizontal.tella.mobile.util.StatusProvider
import timber.log.Timber

@HiltWorker
class WorkerSendFeedBack @AssistedInject constructor(
    @Assisted val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val feedbackRepository: FeedBackRepository,
    private val statusProvider: StatusProvider
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            if (!statusProvider.isOnline()) {
                return@withContext Result.retry()
            }

            val mainKey = try {
                MyApplication.getMainKeyHolder().get().key.encoded
            } catch (e: LifecycleMainKey.MainKeyUnavailableException) {
                Timber.e(e, "Failed to retrieve main key")
                return@withContext Result.retry()
            }

            val dataSource = DataSource.getInstance(context, mainKey)

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
                    Timber.e(e, "Submitting feedback instance failed; will retry")
                    setNoTimeOut(false)
                    return@withContext Result.retry()
                }
            }

            setNoTimeOut(false)
            Result.success()
        } catch (t: Throwable) {
            Timber.e(t, "WorkerSendFeedBack failed unexpectedly")
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
