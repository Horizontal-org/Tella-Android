package org.horizontal.tella.mobile.domain.repository.feedback

import io.reactivex.Single
import org.horizontal.tella.mobile.data.entity.feedback.FeedbackBodyEntity
import org.horizontal.tella.mobile.domain.entity.feedback.FeedbackInstance
import org.horizontal.tella.mobile.domain.entity.feedback.FeedbackPostResult


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