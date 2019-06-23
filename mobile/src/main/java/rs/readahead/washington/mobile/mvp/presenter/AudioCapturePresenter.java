package rs.readahead.washington.mobile.mvp.presenter;

import com.crashlytics.android.Crashlytics;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
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
                .doOnSubscribe(new Consumer<Disposable>() {
                    @Override
                    public void accept(Disposable disposable) throws Exception {
                        view.onAddingStart();
                    }
                })
                .observeOn(AndroidSchedulers.mainThread())
                .doFinally(new Action() {
                    @Override
                    public void run() throws Exception {
                        view.onAddingEnd();
                    }
                })
                .subscribe(new Consumer<MediaFile>() {
                    @Override
                    public void accept(MediaFile mediaFile) throws Exception {
                        view.onAddSuccess(mediaFile.getId());
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        Crashlytics.logException(throwable);
                        view.onAddError(throwable);
                    }
                })
        );
    }

    @Override
    public void destroy() {
        disposables.dispose();
        cacheWordDataSource.dispose();
        view = null;
    }
}
