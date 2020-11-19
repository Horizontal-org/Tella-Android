package rs.readahead.washington.mobile.mvp.presenter;

import com.google.firebase.crashlytics.FirebaseCrashlytics;

import java.util.concurrent.Callable;

import io.reactivex.Completable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import rs.readahead.washington.mobile.data.database.CacheWordDataSource;
import rs.readahead.washington.mobile.domain.entity.MediaFile;
import rs.readahead.washington.mobile.media.MediaFileHandler;
import rs.readahead.washington.mobile.mvp.contract.IMediaFileViewerPresenterContract;


public class MediaFileViewerPresenter implements IMediaFileViewerPresenterContract.IPresenter {
    private IMediaFileViewerPresenterContract.IView view;
    private CompositeDisposable disposables = new CompositeDisposable();
    private CacheWordDataSource cacheWordDataSource;


    public MediaFileViewerPresenter(IMediaFileViewerPresenterContract.IView view) {
        this.view = view;
        this.cacheWordDataSource = new CacheWordDataSource(view.getContext());
    }

    @Override
    public void exportNewMediaFile(final MediaFile mediaFile) {
        disposables.add(Completable.fromCallable((Callable<Void>) () -> {
                    MediaFileHandler.exportMediaFile(view.getContext().getApplicationContext(), mediaFile);
                    return null;
                })
                .subscribeOn(Schedulers.computation())
                .doOnSubscribe(disposable -> view.onExportStarted())
                .observeOn(AndroidSchedulers.mainThread())
                .doFinally(() -> view.onExportEnded())
                .subscribe(() -> view.onMediaExported(), throwable -> {
                    FirebaseCrashlytics.getInstance().recordException(throwable);
                    view.onExportError(throwable);
                })
        );
    }

    @Override
    public void deleteMediaFiles(final MediaFile mediaFile) {
        disposables.add(cacheWordDataSource.getDataSource()
                .subscribeOn(Schedulers.io())
                .flatMapCompletable(dataSource ->
                        dataSource.deleteMediaFile(mediaFile, mediaFile1 ->
                                MediaFileHandler.deleteMediaFile(view.getContext(), mediaFile1)).toCompletable())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(() -> view.onMediaFileDeleted(), throwable -> {
                    FirebaseCrashlytics.getInstance().recordException(throwable);
                    view.onMediaFileDeletionError(throwable);
                })
        );
    }

    @Override
    public void destroy() {
        disposables.dispose();
        view = null;
    }
}
