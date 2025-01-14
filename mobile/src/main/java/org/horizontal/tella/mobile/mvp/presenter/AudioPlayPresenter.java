package org.horizontal.tella.mobile.mvp.presenter;

import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.hzontal.tella_vault.rx.RxVault;

import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import org.horizontal.tella.mobile.MyApplication;
import org.horizontal.tella.mobile.domain.exception.NotFountException;
import org.horizontal.tella.mobile.mvp.contract.IAudioPlayPresenterContract;


public class AudioPlayPresenter implements
        IAudioPlayPresenterContract.IPresenter {
    private IAudioPlayPresenterContract.IView view;
    private RxVault rxVault;
    private CompositeDisposable disposables = new CompositeDisposable();


    public AudioPlayPresenter(IAudioPlayPresenterContract.IView view) {
        this.view = view;
        this.rxVault = MyApplication.rxVault;
    }

    @Override
    public void getMediaFile(final String id) {

        disposables.add(Single
                .fromCallable(() -> MyApplication.rxVault.get(id))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(vaultFile ->
                        {
                            if (vaultFile == null) {
                                view.onMediaFileError(new NotFountException());
                            } else {
                                view.onMediaFileSuccess(vaultFile.blockingGet());
                            }
                        },

                        throwable -> {
                            FirebaseCrashlytics.getInstance().recordException(throwable);
                            view.onMediaFileError(throwable);
                        }));
    }

    @Override
    public void destroy() {
        disposables.dispose();
        view = null;
    }


}
