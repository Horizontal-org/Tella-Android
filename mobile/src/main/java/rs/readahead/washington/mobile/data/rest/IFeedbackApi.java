package rs.readahead.washington.mobile.data.rest;

import io.reactivex.Completable;
import retrofit2.http.Body;
import retrofit2.http.POST;
import rs.readahead.washington.mobile.data.entity.FeedbackEntity;


public interface IFeedbackApi {
    @POST("feedback/messages")
    Completable sendFeedback(@Body FeedbackEntity feedback);
}
