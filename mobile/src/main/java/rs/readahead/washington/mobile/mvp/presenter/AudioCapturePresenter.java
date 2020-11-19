package rs.readahead.washington.mobile.mvp.presenter;

import android.os.Build;
import android.os.Environment;
import android.os.StatFs;

import com.google.firebase.crashlytics.FirebaseCrashlytics;

import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import rs.readahead.washington.mobile.data.database.CacheWordDataSource;
import rs.readahead.washington.mobile.domain.entity.MediaFile;
import rs.readahead.washington.mobile.media.MediaFileHandler;
import rs.readahead.washington.mobile.mvp.contract.IAudioCapturePresenterContract;
import rs.readahead.washington.mobile.presentation.entity.MediaFileThumbnailData;


public class AudioCapturePresenter implements IAudioCapturePresenterContract.IPresenter {
    private IAudioCapturePresenterContract.IView view;
    private CompositeDisposable disposables = new CompositeDisposable();
    private CacheWordDataSource cacheWordDataSource;
    private MediaFileHandler mediaFileHandler;


    public AudioCapturePresenter(IAudioCapturePresenterContract.IView view) {
        this.view = view;
        this.cacheWordDataSource = new CacheWordDataSource(view.getContext());
        this.mediaFileHandler = new MediaFileHandler(cacheWordDataSource);
    }

    @Override
    public void addMediaFile(MediaFile mediaFile) { // audio recorder creates MediaFile's file already encrypted and in place
        disposables.add(mediaFileHandler.registerMediaFile(mediaFile, MediaFileThumbnailData.NONE)
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
        cacheWordDataSource.dispose();
        view = null;
    }
}
