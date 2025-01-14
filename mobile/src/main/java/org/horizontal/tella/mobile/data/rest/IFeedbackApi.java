package org.horizontal.tella.mobile.data.rest;

import io.reactivex.Completable;
import retrofit2.http.Body;
import retrofit2.http.POST;
import org.horizontal.tella.mobile.data.entity.FeedbackEntity;


public interface IFeedbackApi {
    @POST("feedback/messages")
    Completable sendFeedback(@Body FeedbackEntity feedback);
}
