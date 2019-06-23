package rs.readahead.washington.mobile.mvp.presenter;

import com.crashlytics.android.Crashlytics;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import rs.readahead.washington.mobile.data.database.CacheWordDataSource;
import rs.readahead.washington.mobile.domain.entity.MediaFile;
import rs.readahead.washington.mobile.media.MediaFileBundle;
import rs.readahead.washington.mobile.media.MediaFileHandler;
import rs.readahead.washington.mobile.mvp.contract.ISignaturePresenterContract;


public class SignaturePresenter implements ISignaturePresenterContract.IPresenter {
    private ISignaturePresenterContract.IView view;
    private CompositeDisposable disposables = new CompositeDisposable();
    private CacheWordDataSource cacheWordDataSource;
    private MediaFileHandler mediaFileHandler;


    public SignaturePresenter(ISignaturePresenterContract.IView view) {
        this.view = view;
        this.cacheWordDataSource = new CacheWordDataSource(view.getContext());
        this.mediaFileHandler = new MediaFileHandler(cacheWordDataSource);
    }

    @Override
    public void addPngImage(final byte[] png) {
        disposables.add(
                Observable.fromCallable(() -> MediaFileHandler.savePngImage(view.getContext(), png))
                .flatMap((Function<MediaFileBundle, ObservableSource<MediaFile>>) bundle -> mediaFileHandler.registerMediaFile(bundle.getMediaFile(),
                        bundle.getMediaFileThumbnailData()))
                .subscribeOn(Schedulers.io())
                .doOnSubscribe(disposable -> view.onAddingStart())
                .observeOn(AndroidSchedulers.mainThread())
                .doFinally(() -> view.onAddingEnd())
                .subscribe(mediaFile -> view.onAddSuccess(mediaFile), throwable -> {
                    Crashlytics.logException(throwable);
                    view.onAddError(throwable);
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
