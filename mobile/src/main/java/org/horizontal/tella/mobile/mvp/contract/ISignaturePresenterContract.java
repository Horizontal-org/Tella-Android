package org.horizontal.tella.mobile.mvp.contract;

import android.content.Context;

import com.hzontal.tella_vault.VaultFile;

public class ISignaturePresenterContract {
    public interface IView {
        void onAddingStart();
        void onAddingEnd();
        void onAddSuccess(VaultFile vaultFile);
        void onAddError(Throwable error);
        Context getContext();
    }

    public interface IPresenter extends IBasePresenter {
        void addPngImage(byte[] png);
    }
}
