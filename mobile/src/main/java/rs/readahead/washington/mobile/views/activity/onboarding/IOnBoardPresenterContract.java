package rs.readahead.washington.mobile.views.activity.onboarding;

import rs.readahead.washington.mobile.domain.entity.TellaUploadServer;
import rs.readahead.washington.mobile.domain.entity.collect.CollectServer;
import rs.readahead.washington.mobile.mvp.contract.IBasePresenter;

public interface IOnBoardPresenterContract {
    interface IView {
        void showLoading();
        void hideLoading();
        void onCreatedTUServer(TellaUploadServer server);
        void onCreateTUServerError(Throwable throwable);
        void onCreatedServer(CollectServer server);
        void onCreateCollectServerError(Throwable throwable);
    }

    interface IPresenter extends IBasePresenter {
        void create(TellaUploadServer server);
        void create(CollectServer server);
    }
}
