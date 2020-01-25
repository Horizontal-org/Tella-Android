package rs.readahead.washington.mobile.mvp.contract;

import android.content.Context;
import androidx.annotation.NonNull;

import rs.readahead.washington.mobile.domain.entity.Feedback;


public class IFeedbackPresenterContract {
    public interface IView {
        void onSentFeedback();
        void onFeedbackSendStarted();
        void onFeedbackSendFinished();
        void onSendFeedbackError(Throwable throwable);
        Context getContext();
    }

    public interface IPresenter extends IBasePresenter {
        void sendFeedback(@NonNull Feedback feedback);
    }
}
