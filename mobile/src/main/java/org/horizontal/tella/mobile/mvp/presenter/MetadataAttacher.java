package org.horizontal.tella.mobile.mvp.presenter;

import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.hzontal.tella_vault.Metadata;
import com.hzontal.tella_vault.VaultFile;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import org.horizontal.tella.mobile.MyApplication;
import org.horizontal.tella.mobile.mvp.contract.IMetadataAttachPresenterContract;


public class MetadataAttacher implements IMetadataAttachPresenterContract.IPresenter {
    private IMetadataAttachPresenterContract.IView view;
    private final CompositeDisposable disposables = new CompositeDisposable();


    public MetadataAttacher(IMetadataAttachPresenterContract.IView view) {
        this.view = view;
    }

    @Override
    public void attachMetadata(VaultFile vaultFile, Metadata metadata) {
        disposables.add(MyApplication.rxVault.updateMetadata(vaultFile, metadata)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(updatedVaultFile -> view.onMetadataAttached(updatedVaultFile), throwable -> {
                    FirebaseCrashlytics.getInstance().recordException(throwable);
                    view.onMetadataAttachError(throwable);
                }));
    }

    @Override
    public void destroy() {
        disposables.dispose();
        view = null;
    }
}
