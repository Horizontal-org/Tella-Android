package org.horizontal.tella.mobile.views.activity.onboarding;

import org.horizontal.tella.mobile.domain.entity.reports.TellaReportServer;
import org.horizontal.tella.mobile.domain.entity.UWaziUploadServer;
import org.horizontal.tella.mobile.domain.entity.collect.CollectServer;
import org.horizontal.tella.mobile.mvp.contract.IBasePresenter;

public interface IOnBoardPresenterContract {
    interface IView {
        void showLoading();
        void hideLoading();
        void onCreatedTUServer(TellaReportServer server);
        void onCreateTUServerError(Throwable throwable);
        void onCreatedServer(CollectServer server);
        void onCreatedUwaziServer(UWaziUploadServer server);
        void onCreateCollectServerError(Throwable throwable);
    }

    interface IPresenter extends IBasePresenter {
        void create(TellaReportServer server);
        void create(CollectServer server);
        void create(UWaziUploadServer server);
    }
}
