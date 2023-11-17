package rs.readahead.washington.mobile.data.feedback.repository

import android.annotation.SuppressLint
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import rs.readahead.washington.mobile.data.database.DataSource
import rs.readahead.washington.mobile.data.entity.feedback.FeedbackBodyEntity
import rs.readahead.washington.mobile.data.entity.feedback.mapToDomainModel
import rs.readahead.washington.mobile.data.feedback.remote.FeedbackApiService
import rs.readahead.washington.mobile.domain.entity.feedback.FeedbackInstance
import rs.readahead.washington.mobile.domain.entity.feedback.FeedbackPostResult
import rs.readahead.washington.mobile.domain.entity.feedback.FeedbackStatus
import rs.readahead.washington.mobile.domain.exception.NoConnectivityException
import rs.readahead.washington.mobile.domain.repository.feedback.FeedBackRepository
import javax.inject.Inject

class FeedbackRepositoryImp @Inject internal constructor(
        private val apiService: FeedbackApiService, private val dataSource: DataSource) : FeedBackRepository {
    companion object {
        private const val TELLA_PLATFORM = "wearehorizontal"
    }

    override fun submitFeedback(feedbackBody: FeedbackBodyEntity): Single<FeedbackPostResult> {
        return apiService.submitFeedback(
                data = feedbackBody,
                tellaPlatform = TELLA_PLATFORM,
        ).map { it.mapToDomainModel() }
    }

    @SuppressLint("CheckResult")
    override fun submitFeedback(feedbackInstance: FeedbackInstance): Single<FeedbackPostResult> {
        return submitFeedback(FeedbackBodyEntity(feedbackInstance.platform, feedbackInstance.text))
                .doOnError { throwable ->
                    handleSubmissionError(feedbackInstance, throwable)
                }.doOnSuccess {
                    dataSource.deleteFeedbackInstance(feedbackInstance.id)
                            .subscribeOn(Schedulers.io()).subscribe({
                            }, { throwable ->
                                throwable.printStackTrace()
                            })
                }
    }

    override fun displayFeedbackSent(): Single<Boolean> {
        return Single.fromCallable {
            true
        }
    }


    private fun handleSubmissionError(feedbackInstance: FeedbackInstance, throwable: Throwable) {
        feedbackInstance.status = if (throwable is NoConnectivityException) {
            FeedbackStatus.SUBMISSION_PENDING
        } else {
            FeedbackStatus.SUBMISSION_ERROR
        }
        dataSource.saveInstance(feedbackInstance).subscribe()
    }


}