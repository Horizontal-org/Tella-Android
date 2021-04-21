package rs.readahead.washington.mobile.mvp.presenter;

import android.os.Build;
import android.os.Environment;
import android.os.StatFs;

import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.hzontal.tella_vault.VaultFile;

import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import rs.readahead.washington.mobile.MyApplication;
import rs.readahead.washington.mobile.data.database.KeyDataSource;
import rs.readahead.washington.mobile.media.MediaFileHandler;
import rs.readahead.washington.mobile.mvp.contract.IAudioCapturePresenterContract;


public class AudioCapturePresenter implements IAudioCapturePresenterContract.IPresenter {
    private IAudioCapturePresenterContract.IView view;
    private CompositeDisposable disposables = new CompositeDisposable();
    private KeyDataSource keyDataSource;
    private MediaFileHandler mediaFileHandler;


    public AudioCapturePresenter(IAudioCapturePresenterContract.IView view) {
        this.view = view;
        this.keyDataSource = MyApplication.getKeyDataSource();
        this.mediaFileHandler = new MediaFileHandler(keyDataSource);
    }

    @Override
    public void addMediaFile(VaultFile vaultFile) { // audio recorder creates MediaFile's file already encrypted and in place
        disposables.add(mediaFileHandler.registerMediaFile(vaultFile)
                .subscribeOn(Schedulers.io())
                .doOnSubscribe(disposable -> view.onAddingStart())
                .observeOn(AndroidSchedulers.mainThread())
                .doFinally(() -> view.onAddingEnd())
                .subscribe(mediaFile1 -> view.onAddSuccess(mediaFile1), throwable -> {
                    FirebaseCrashlytics.getInstance().recordException(throwable);
                    view.onAddError(throwable);
                })
        );
    }

    @Override
    public void checkAvailableStorage() {
        disposables.add(Single.fromCallable(() -> {
                    StatFs statFs = new StatFs(Environment.getExternalStorageDirectory().getAbsolutePath());
                    long freeSpace;

                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2) {
                        freeSpace = (statFs.getAvailableBlocks() * statFs.getBlockSize());
                    } else {
                        freeSpace = (statFs.getAvailableBlocksLong() * statFs.getBlockSizeLong());
                    }

                    return freeSpace;
                })
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(free -> view.onAvailableStorage(free),
                                throwable -> view.onAvailableStorageFailed(throwable))
        );
    }

    @Override
    public void destroy() {
        disposables.dispose();
        view = null;
    }
}
