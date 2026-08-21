package org.horizontal.tella.mobile.mvp.contract;

import android.content.Context;

import com.hzontal.tella_vault.VaultFile;

public class ICollectAttachmentMediaFilePresenterContract {
    public interface IView {
        void onGetMediaFileSuccess(VaultFile vaultFile);
        void onGetMediaFileStart();
        void onGetMediaFileEnd();
        void onGetMediaFileError(Throwable error);
        Context getContext();
    }

    public interface IPresenter extends IBasePresenter {
        void getMediaFile(String fileName);
    }
}
