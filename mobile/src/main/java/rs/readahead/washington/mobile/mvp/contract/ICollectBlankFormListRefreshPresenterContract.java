package rs.readahead.washington.mobile.mvp.contract;

import android.content.Context;

public class ICollectBlankFormListRefreshPresenterContract {
    public interface IView {
        void onRefreshBlankFormsError(Throwable error);
        Context getContext();
    }

    public interface IPresenter extends IBasePresenter {
        void refreshBlankForms();
    }
}