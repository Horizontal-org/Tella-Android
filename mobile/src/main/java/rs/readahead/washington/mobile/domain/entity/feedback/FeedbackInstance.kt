package rs.readahead.washington.mobile.domain.entity.feedback

import rs.readahead.washington.mobile.domain.entity.EntityStatus
import rs.readahead.washington.mobile.domain.entity.collect.FormMediaFile
import rs.readahead.washington.mobile.domain.entity.collect.FormMediaFileStatus
import java.io.Serializable

data class FeedbackInstance (
        var id: Long = -1,
        var status: FeedbackStatus = FeedbackStatus.UNKNOWN,
        var text: String = "",
) : Serializable