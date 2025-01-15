package org.horizontal.tella.mobile.util.jobs

import android.annotation.SuppressLint
import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.RxWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import io.reactivex.Single
import org.hzontal.tella.keys.key.LifecycleMainKey
import org.horizontal.tella.mobile.MyApplication
import org.horizontal.tella.mobile.data.database.DataSource
import org.horizontal.tella.mobile.domain.repository.feedback.FeedBackRepository
import org.horizontal.tella.mobile.util.LockTimeoutManager
import org.horizontal.tella.mobile.util.StatusProvider
import timber.log.Timber


@HiltWorker
class WorkerSendFeedBack @AssistedInject constructor(@Assisted val context: Context, @Assisted workerParams: WorkerParameters, private val feedbackRepository: FeedBackRepository, private val statusProvider: StatusProvider) : RxWorker(context, workerParams) {

    @SuppressLint("RestrictedApi")
    override fun createWork(): Single<Result> {
        return Single.fromCallable {
            if (!statusProvider.isOnline()) {
                return@fromCallable Result.retry()
            }
            val mainKey = try {
                MyApplication.getMainKeyHolder().get().key.encoded
            } catch (e: LifecycleMainKey.MainKeyUnavailableException) {
                Timber.e(e, "Failed to retrieve main key")
                return@fromCallable Result.retry()
            }
            val dataSource = DataSource.getInstance(context, mainKey)
            val feedbackInstances = dataSource.listFeedBackInstances()?.blockingGet()

            if (feedbackInstances?.isEmpty() == true) {
                setNoTimeOut(false)
                return@fromCallable Result.success()
            } else {
                setNoTimeOut(true)
            }
            if (feedbackInstances != null) {
                for (feedbackInstance in feedbackInstances) {
                    feedbackRepository.submitFeedbackInstance(feedbackInstance).blockingGet()
                }
            }
            setNoTimeOut(false)
            return@fromCallable Result.success().apply { }
        }.onErrorReturn { error ->
            Timber.e(error, "WorkerSendFeedBack failed")
            Result.retry()
        }
    }

    private fun setNoTimeOut(enableNoTimeout: Boolean) {
        if (enableNoTimeout) {
            MyApplication.getMainKeyHolder().timeout = LifecycleMainKey.NO_TIMEOUT
        } else {
            MyApplication.getMainKeyHolder().timeout = LockTimeoutManager.IMMEDIATE_SHUTDOWN
        }
    }
}