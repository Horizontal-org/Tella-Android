package rs.readahead.washington.mobile.mvp.contract;

import android.content.Context;


public class IHomeScreenPresenterContract {
    public interface IView {
        Context getContext();
    }

    public interface IPresenter extends IBasePresenter {
        void executePanicMode();
    }
}
