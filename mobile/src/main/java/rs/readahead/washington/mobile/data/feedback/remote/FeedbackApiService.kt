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

        companion object {
            //base URL
            const val BASE_URL = "https://api.feedback.tella-app.org/"
        }

        @POST
        fun submitFeedback(
                @Url
                url: String,
                @Header(ParamsNetwork.FEEDBACK_HEADER) tella_platform: String,
                @Body
                feedbackBodyEntity: FeedbackBodyEntity,
        ): Single<FeedbackPostResponse>

    }
