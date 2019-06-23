package rs.readahead.washington.mobile.mvp.presenter;

import android.support.annotation.NonNull;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import rs.readahead.washington.mobile.data.repository.FeedbackRepository;
import rs.readahead.washington.mobile.domain.entity.Feedback;
import rs.readahead.washington.mobile.domain.repository.IFeedbackRepository;
import rs.readahead.washington.mobile.mvp.contract.IFeedbackPresenterContract;


public class FeedbackPresenter implements IFeedbackPresenterContract.IPresenter {
    private IFeedbackPresenterContract.IView view;
    private IFeedbackRepository feedbackRepository;
    private CompositeDisposable disposable;


    public FeedbackPresenter(IFeedbackPresenterContract.IView view) {
        this.view = view;
        feedbackRepository = new FeedbackRepository();
        disposable = new CompositeDisposable();
    }

    @Override
    public void sendFeedback(@NonNull final Feedback feedback) {
        disposable.add(feedbackRepository.sendFeedback(feedback)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe(new Consumer<Disposable>() {
                    @Override
                    public void accept(@NonNull Disposable disposable) throws Exception {
                        view.onFeedbackSendStarted();
                    }
                })
                .doFinally(new Action() {
                    @Override
                    public void run() throws Exception {
                        view.onFeedbackSendFinished();
                    }
                })
                .subscribe(new Action() {
                    @Override
                    public void run() throws Exception {
                        view.onSentFeedback();
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        view.onSendFeedbackError(throwable);
                    }
                })
        );
    }

    @Override
    public void destroy() {
        disposable.dispose();
        view = null;
    }
}
