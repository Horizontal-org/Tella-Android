package org.horizontal.tella.mobile.mvp.contract;

import android.content.Context;


public class IHomeScreenPresenterContract {
    public interface IView {
        Context getContext();
        void onCountTUServersEnded(Long num);
        void onCountTUServersFailed(Throwable throwable);
        void onCountCollectServersEnded(Long num);
        void onCountCollectServersFailed(Throwable throwable);
        void onCountUwaziServersEnded(Long num);
        void onCountUwaziServersFailed(Throwable throwable);
    }

    public interface IPresenter extends IBasePresenter {
        void executePanicMode();
        void countTUServers();
        void countCollectServers();
        void countUwaziServers();
    }
}
