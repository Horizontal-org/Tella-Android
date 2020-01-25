package rs.readahead.washington.mobile.data.repository;

import androidx.annotation.NonNull;

import io.reactivex.Completable;
import io.reactivex.CompletableSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import rs.readahead.washington.mobile.data.entity.mapper.EntityMapper;
import rs.readahead.washington.mobile.data.rest.FeedbackApi;
import rs.readahead.washington.mobile.domain.entity.Feedback;
import rs.readahead.washington.mobile.domain.repository.IFeedbackRepository;


public class FeedbackRepository implements IFeedbackRepository {

    @Override
    public Completable sendFeedback(@NonNull Feedback feedback) {
        return FeedbackApi.getApi().sendFeedback(new EntityMapper().transform(feedback))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .onErrorResumeNext(new Function<Throwable, CompletableSource>() {
                    @Override
                    public CompletableSource apply(Throwable throwable) throws Exception {
                        return Completable.error(new ErrorBundle(throwable));
                    }
                });
    }
}
