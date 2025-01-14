package org.horizontal.tella.mobile.mvp.contract;

import android.content.Context;

import com.hzontal.tella_vault.Metadata;
import com.hzontal.tella_vault.VaultFile;

public class IMetadataAttachPresenterContract {
    public interface IView {
        void onMetadataAttached(VaultFile vaultFile);
        void onMetadataAttachError(Throwable throwable);
        Context getContext();
    }

    public interface IPresenter extends IBasePresenter {
        void attachMetadata(VaultFile vaultFile, Metadata metadata);
    }
}
