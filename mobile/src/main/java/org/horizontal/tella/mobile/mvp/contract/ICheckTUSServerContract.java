package org.horizontal.tella.mobile.mvp.contract;

import android.content.Context;

import org.horizontal.tella.mobile.domain.entity.reports.TellaReportServer;
import org.horizontal.tella.mobile.domain.entity.UploadProgressInfo;


public class ICheckTUSServerContract {
    public interface IView {
        void onServerCheckSuccess(TellaReportServer server);
        void onServerCheckFailure(UploadProgressInfo.Status status);
        void onServerCheckError(Throwable error);
        void showServerCheckLoading();
        void hideServerCheckLoading();
        void onNoConnectionAvailable();
        void setSaveAnyway(boolean enabled);
        Context getContext();
    }

    public interface IPresenter extends IBasePresenter {
        void checkServer(TellaReportServer server, boolean connectionRequired);
    }
}
