package org.horizontal.tella.mobile.mvp.contract;

import android.content.Context;

import org.horizontal.tella.mobile.domain.entity.IErrorBundle;
import org.horizontal.tella.mobile.domain.entity.collect.CollectServer;


public class ICheckOdkServerContract {
    public interface IView {
        void onServerCheckSuccess(CollectServer server);
        void onServerCheckFailure(IErrorBundle errorBundle);
        void onServerCheckError(Throwable error);
        void showServerCheckLoading();
        void hideServerCheckLoading();
        void onNoConnectionAvailable();
        void setSaveAnyway(boolean enabled);
        Context getContext();
    }

    public interface IPresenter extends IBasePresenter {
        void checkServer(CollectServer server, boolean connectionRequired);
    }
}
