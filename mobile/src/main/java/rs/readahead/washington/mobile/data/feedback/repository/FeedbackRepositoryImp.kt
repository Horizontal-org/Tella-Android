package rs.readahead.washington.mobile.data.feedback.repository

import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import rs.readahead.washington.mobile.data.database.DataSource
import rs.readahead.washington.mobile.data.entity.feedback.FeedbackBodyEntity
import rs.readahead.washington.mobile.data.entity.feedback.mapToDomainModel
import rs.readahead.washington.mobile.data.feedback.remote.FeedbackApiService
import rs.readahead.washington.mobile.domain.entity.feedback.FeedbackPostResult
import rs.readahead.washington.mobile.domain.repository.feedback.FeedbackRepository
import javax.inject.Inject

class FeedbackRepositoryImp @Inject internal constructor(
    private val apiService: FeedbackApiService,
    private val dataSource: DataSource
) : FeedbackRepository {
    override fun submitFeedback(feedbackBody: FeedbackBodyEntity): Single<FeedbackPostResult> {
        return apiService.submitFeedback(
            data = feedbackBody,
            tellaPlatform = "wearehorizontal",
        ).map { it.mapToDomainModel() }
    }

}