package org.horizontal.tella.mobile.data.repository;

import androidx.annotation.NonNull;

import io.reactivex.Completable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import org.horizontal.tella.mobile.data.entity.mapper.EntityMapper;
import org.horizontal.tella.mobile.data.rest.FeedbackApi;
import org.horizontal.tella.mobile.domain.entity.Feedback;
import org.horizontal.tella.mobile.domain.repository.IFeedbackRepository;


public class FeedbackRepository implements IFeedbackRepository {

    @Override
    public Completable sendFeedback(@NonNull Feedback feedback) {
        return FeedbackApi.getApi().sendFeedback(new EntityMapper().transform(feedback))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .onErrorResumeNext(throwable -> Completable.error(new ErrorBundle(throwable)));
    }
}
