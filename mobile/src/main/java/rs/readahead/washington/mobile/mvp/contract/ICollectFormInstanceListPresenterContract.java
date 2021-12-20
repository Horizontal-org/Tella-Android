package rs.readahead.washington.mobile.mvp.contract;

import android.content.Context;

import java.util.List;

import rs.readahead.washington.mobile.domain.entity.collect.CollectFormInstance;


public class ICollectFormInstanceListPresenterContract {
    public interface IView {
        void onFormInstanceListSuccess(List<CollectFormInstance> forms);
        void onFormInstanceListError(Throwable error);
        Context getContext();
    }

    public interface IPresenter extends IBasePresenter {
        void listDraftFormInstances();
        void listSubmitFormInstances();
        void listOutboxFormInstances();
    }
}
