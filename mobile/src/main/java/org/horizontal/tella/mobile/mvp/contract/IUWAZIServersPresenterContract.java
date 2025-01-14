package org.horizontal.tella.mobile.mvp.contract;

import android.content.Context;

import java.util.List;

import org.horizontal.tella.mobile.domain.entity.UWaziUploadServer;

public class IUWAZIServersPresenterContract {

  public interface IView {
        Context getContext();
        void showLoading();
        void hideLoading();
        void onUwaziServersLoaded(List<UWaziUploadServer> uzServers);
        void onLoadUwaziServersError(Throwable throwable);
        void onCreatedUwaziServer(UWaziUploadServer server);
        void onCreateUwaziServerError(Throwable throwable);
        void onRemovedUwaziServer(UWaziUploadServer server);
        void onRemoveUwaziServerError(Throwable throwable);
        void onUpdatedUwaziServer(UWaziUploadServer server);
        void onUpdateUwaziServerError(Throwable throwable);
    }

    public interface IPresenter extends IBasePresenter {
        void getUwaziServers();
        void create(UWaziUploadServer server);
        void update(UWaziUploadServer server);
        void remove(UWaziUploadServer server);
    }
}
