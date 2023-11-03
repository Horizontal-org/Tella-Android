package rs.readahead.washington.mobile.domain.repository.feedback

import io.reactivex.Single
import rs.readahead.washington.mobile.data.entity.feedback.FeedbackBodyEntity
import rs.readahead.washington.mobile.domain.entity.feedback.FeedbackInstance
import rs.readahead.washington.mobile.domain.entity.feedback.FeedbackPostResult


interface FeedBackRepository {
    fun submitFeedback(
            feedbackBody: FeedbackBodyEntity
    ): Single<FeedbackPostResult>

    fun submitFeedback(
            feedbackBody: FeedbackInstance
    ): Single<FeedbackPostResult>
}