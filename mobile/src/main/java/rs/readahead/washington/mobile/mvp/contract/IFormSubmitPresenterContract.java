package rs.readahead.washington.mobile.mvp.contract;

import android.content.Context;

import rs.readahead.washington.mobile.domain.entity.collect.CollectFormInstance;


public class IFormSubmitPresenterContract {
    public interface IView {
        void onGetFormInstanceSuccess(CollectFormInstance instance);
        void onGetFormInstanceError(Throwable throwable);
        Context getContext();
    }

    public interface IPresenter extends IBasePresenter {
        void getFormInstance(long instanceId);
    }
}
