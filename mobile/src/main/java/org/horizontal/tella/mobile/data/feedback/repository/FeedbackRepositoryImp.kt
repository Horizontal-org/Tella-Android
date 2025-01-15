package org.horizontal.tella.mobile.data.feedback.repository

import android.annotation.SuppressLint
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import org.horizontal.tella.mobile.data.ParamsNetwork
import org.horizontal.tella.mobile.data.database.DataSource
import org.horizontal.tella.mobile.data.entity.feedback.FeedbackBodyEntity
import org.horizontal.tella.mobile.data.entity.feedback.mapToDomainModel
import org.horizontal.tella.mobile.data.feedback.remote.FeedbackApiService
import org.horizontal.tella.mobile.domain.entity.feedback.FeedbackInstance
import org.horizontal.tella.mobile.domain.entity.feedback.FeedbackPostResult
import org.horizontal.tella.mobile.domain.entity.feedback.FeedbackStatus
import org.horizontal.tella.mobile.domain.exception.NoConnectivityException
import org.horizontal.tella.mobile.domain.repository.feedback.FeedBackRepository
import javax.inject.Inject

class FeedbackRepositoryImp @Inject internal constructor(
    private val apiService: FeedbackApiService,
    private val dataSource: DataSource
) : FeedBackRepository {

    override fun submitFeedback(feedbackBody: FeedbackBodyEntity): Single<FeedbackPostResult> {
        return apiService.submitFeedback(
            data = feedbackBody,
            tellaPlatform = ParamsNetwork.TELLA_PLATFORM,
        ).map { response -> response.mapToDomainModel() }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnError { throwable -> throwable.printStackTrace() }
    }

    @SuppressLint("CheckResult")
    override fun submitFeedbackInstance(feedbackBody: FeedbackInstance): Single<FeedbackPostResult> {
        return submitFeedback(
            FeedbackBodyEntity(
                feedbackBody.platform,
                feedbackBody.text
            )
        ).doOnError { throwable ->
            handleSubmissionError(feedbackBody, throwable)
        }.doOnSuccess {
            dataSource.deleteFeedbackInstance(feedbackBody.id).subscribeOn(Schedulers.io())
                .subscribe({}, { throwable ->
                    throwable.printStackTrace()
                })
        }
    }

    override fun saveFeedbackInstance(feedbackInstance: FeedbackInstance): Single<FeedbackInstance> {
        return dataSource.saveFeedbackInstance(feedbackInstance)
    }

    override fun getFeedbackDraft(): Single<FeedbackInstance> {
        return dataSource.getFeedbackDraft()
    }

    private fun handleSubmissionError(feedbackInstance: FeedbackInstance, throwable: Throwable) {
        feedbackInstance.status = if (throwable is NoConnectivityException) {
            FeedbackStatus.SUBMISSION_PENDING
        } else {
            FeedbackStatus.SUBMISSION_ERROR
        }
        dataSource.saveFeedbackInstance(feedbackInstance).subscribe()
    }
}