package rs.readahead.washington.mobile.data.feedback.remote

import io.reactivex.Single
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Url
import rs.readahead.washington.mobile.data.entity.feedback.FeedbackBodyEntity
import rs.readahead.washington.mobile.data.entity.feedback.FeedbackPostResponse
import rs.readahead.washington.mobile.data.feedback.utils.ParamsNetwork

@JvmSuppressWildcards
interface FeedbackApiService {


    @POST
    fun submitFeedback(
            @Url
            url: String = "https://api.feedback.tella-app.org/",
            @Header(ParamsNetwork.FEEDBACK_HEADER) tellaPlatform: String,
            @Body
            feedbackBodyEntity: FeedbackBodyEntity,
    ): Single<FeedbackPostResponse>

}
