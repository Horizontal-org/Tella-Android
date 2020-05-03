package rs.readahead.washington.mobile.mvp.contract;

import android.content.Context;


public class IHomeScreenPresenterContract {
    public interface IView {
        Context getContext();
        void onCountTUServersEnded(Long num);
        void onCountTUServersFailed(Throwable throwable);
    }

    public interface IPresenter extends IBasePresenter {
        void executePanicMode();
        void countTUServers();
    }
}
