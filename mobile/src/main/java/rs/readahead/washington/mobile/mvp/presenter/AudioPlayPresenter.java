package rs.readahead.washington.mobile.mvp.presenter;

//import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.hzontal.tella_vault.rx.RxVault;

import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import rs.readahead.washington.mobile.MyApplication;
import rs.readahead.washington.mobile.domain.exception.NotFountException;
import rs.readahead.washington.mobile.mvp.contract.IAudioPlayPresenterContract;
import timber.log.Timber;


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
                            Timber.e(throwable);
                            view.onMediaFileError(throwable);
                        }));
    }

    @Override
    public void destroy() {
        disposables.dispose();
        view = null;
    }


}
