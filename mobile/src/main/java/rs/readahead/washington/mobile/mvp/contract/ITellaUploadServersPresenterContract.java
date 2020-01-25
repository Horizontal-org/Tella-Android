package rs.readahead.washington.mobile.mvp.contract;

import android.content.Context;

import java.util.List;

import rs.readahead.washington.mobile.domain.entity.TellaUploadServer;


public class ITellaUploadServersPresenterContract {
    public interface IView {
        Context getContext();
        void showLoading();
        void hideLoading();
        void onTUServersLoaded(List<TellaUploadServer> servers);
        void onLoadTUServersError(Throwable throwable);
        void onCreatedTUServer(TellaUploadServer server);
        void onCreateTUServerError(Throwable throwable);
        void onRemovedTUServer(TellaUploadServer server);
        void onRemoveTUServerError(Throwable throwable);
        void onUpdatedTUServer(TellaUploadServer server);
        void onUpdateTUServerError(Throwable throwable);
    }

    public interface IPresenter extends IBasePresenter {
        void getTUServers();
        void create(TellaUploadServer server);
        void update(TellaUploadServer server);
        void remove(TellaUploadServer server);
    }
}