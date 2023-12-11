package rs.readahead.washington.mobile.domain.repository.feedback

import io.reactivex.Single
import rs.readahead.washington.mobile.data.entity.feedback.FeedbackBodyEntity
import rs.readahead.washington.mobile.domain.entity.feedback.FeedbackInstance
import rs.readahead.washington.mobile.domain.entity.feedback.FeedbackPostResult


interface FeedBackRepository {
    fun submitFeedback(
        feedbackBody: FeedbackBodyEntity
    ): Single<FeedbackPostResult>

    fun submitFeedbackInstance(
        feedbackBody: FeedbackInstance
    ): Single<FeedbackPostResult>

    fun saveFeedbackInstance(
        feedbackInstance: FeedbackInstance
    )
            : Single<FeedbackInstance>

    fun getFeedbackDraft()
            : Single<FeedbackInstance>
}