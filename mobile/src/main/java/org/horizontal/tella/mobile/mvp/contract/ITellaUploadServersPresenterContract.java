package org.horizontal.tella.mobile.mvp.contract;

import android.content.Context;

import java.util.List;

import org.horizontal.tella.mobile.domain.entity.reports.TellaReportServer;


public class ITellaUploadServersPresenterContract {
    public interface IView {
        Context getContext();
        void showLoading();
        void hideLoading();
        void onTUServersLoaded(List<TellaReportServer> servers);
        void onLoadTUServersError(Throwable throwable);
        void onCreatedTUServer(TellaReportServer server);
        void onCreateTUServerError(Throwable throwable);
        void onRemovedTUServer(TellaReportServer server);
        void onRemoveTUServerError(Throwable throwable);
        void onUpdatedTUServer(TellaReportServer server);
        void onUpdateTUServerError(Throwable throwable);
    }

    public interface IPresenter extends IBasePresenter {
        void getTUServers();
        void create(TellaReportServer server);
        void update(TellaReportServer server);
        void remove(TellaReportServer server);
    }
}