package rs.readahead.washington.mobile.domain.repository.feedback

import io.reactivex.Completable
import io.reactivex.Single
import rs.readahead.washington.mobile.domain.entity.feedback.FeedbackInstance

interface ITellaFeedBackRepository {
    fun saveFeedbackInstance(instance: FeedbackInstance): Single<FeedbackInstance>
    fun getFeedbackDraft(): Single<FeedbackInstance>
    fun deleteFeedbackInstance(id: Long): Completable
    fun listFeedBackInstances(): Single<List<FeedbackInstance>>?
}