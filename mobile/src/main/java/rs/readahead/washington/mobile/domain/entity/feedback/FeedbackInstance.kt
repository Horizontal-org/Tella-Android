package rs.readahead.washington.mobile.domain.entity.feedback

import java.io.Serializable

data class FeedbackInstance(
        var id: Long = -1,
        var status: FeedbackStatus = FeedbackStatus.UNKNOWN,
        var text: String = "",
        var platform: String = "ANDROID"
) : Serializable



