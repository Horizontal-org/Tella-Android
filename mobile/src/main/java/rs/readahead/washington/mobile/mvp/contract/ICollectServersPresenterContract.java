package rs.readahead.washington.mobile.mvp.contract;

import android.content.Context;

import java.util.List;

import rs.readahead.washington.mobile.domain.entity.collect.CollectServer;


public class ICollectServersPresenterContract {
    public interface IView {
        Context getContext();
        void showLoading();
        void hideLoading();
        void onServersLoaded(List<CollectServer> servers);
        void onLoadServersError(Throwable throwable);
        void onCreatedServer(CollectServer server);
        void onCreateServerError(Throwable throwable);
        void onRemovedServer(CollectServer server);
        void onRemoveServerError(Throwable throwable);
        void onUpdatedServer(CollectServer server);
        void onUpdateServerError(Throwable throwable);
    }

    public interface IPresenter extends IBasePresenter {
        void getCollectServers();
        void create(CollectServer server);
        void update(CollectServer server);
        void remove(CollectServer server);
    }
}
