package rs.readahead.washington.mobile.views.fragment.vault;

import android.content.Context;

import rs.readahead.washington.mobile.mvp.contract.IBasePresenter;

public class IHomeVaultPresenter {

    public interface IView {
        Context getContext();
    }
    public interface IPresenter extends IBasePresenter {
        void executePanicMode();
    }
}

