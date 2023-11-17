package rs.readahead.washington.mobile.domain.repository.feedback

import io.reactivex.Completable
import io.reactivex.Single
import rs.readahead.washington.mobile.domain.entity.feedback.FeedbackInstance

interface ITellaFeedBackRepository {
    fun saveInstance(instance: FeedbackInstance): Single<FeedbackInstance>
    fun getFeedbackDraft(): Single<FeedbackInstance>
    fun  deleteFeedbackInstance(id:Long): Completable
    fun listFeedBackInstancesToBeSubmitted(): Single<List<FeedbackInstance>>?


}