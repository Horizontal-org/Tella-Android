package rs.readahead.washington.mobile.mvp.presenter;

import com.crashlytics.android.Crashlytics;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Flowable;
import io.reactivex.SingleSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import rs.readahead.washington.mobile.data.database.CacheWordDataSource;
import rs.readahead.washington.mobile.data.database.DataSource;
import rs.readahead.washington.mobile.data.upload.TUSClient;
import rs.readahead.washington.mobile.domain.entity.MediaFile;
import rs.readahead.washington.mobile.domain.entity.RawFile;
import rs.readahead.washington.mobile.domain.entity.TellaUploadServer;
import rs.readahead.washington.mobile.media.MediaFileHandler;
import rs.readahead.washington.mobile.mvp.contract.IFileUploadingPresenterContract;
import timber.log.Timber;

public class FileUploadingPresenter implements IFileUploadingPresenterContract.IPresenter {
    private IFileUploadingPresenterContract.IView view;
    private CompositeDisposable disposables = new CompositeDisposable();
    private CacheWordDataSource cacheWordDataSource;

    public FileUploadingPresenter(IFileUploadingPresenterContract.IView view) {
        this.view = view;
        this.cacheWordDataSource = new CacheWordDataSource(view.getContext());
    }

    @Override
    public void getMediaFiles(final long[] ids, boolean metadata) {
        disposables.add(cacheWordDataSource.getDataSource()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .flatMapSingle((Function<DataSource, SingleSource<List<MediaFile>>>) dataSource -> dataSource.getMediaFiles(ids))
                .map(mediaFiles -> {
                    List<RawFile> rawFiles = new ArrayList<>(mediaFiles.size() * (metadata ? 2 : 1));

                    for (MediaFile mediaFile: mediaFiles) {
                        rawFiles.add(mediaFile);

                        if (metadata) {
                            try {
                                rawFiles.add(MediaFileHandler.maybeCreateMetadataMediaFile(view.getContext(), mediaFile));
                            } catch (Exception e) {
                                Timber.d(e);
                            }
                        }
                    }

                    return rawFiles;
                })
                .subscribe(mediaFiles -> view.onGetMediaFilesSuccess(mediaFiles), throwable -> {
                    Timber.d(throwable);
                    Crashlytics.logException(throwable);
                    view.onGetMediaFilesError(throwable);
                })
        );
    }

    @Override
    public void uploadMediaFiles(TellaUploadServer server, List<RawFile> mediaFiles, boolean metadata) {
        final TUSClient tusClient = new TUSClient(view.getContext(), server.getUrl(), server.getUsername(), server.getPassword());

        disposables.add(Flowable.fromIterable(mediaFiles)
                .flatMap(tusClient::upload)
                .subscribeOn(Schedulers.single())
                .doOnSubscribe(disposable -> view.onMediaFilesUploadStarted())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnComplete(() -> {
                })
                .doFinally(() -> view.onMediaFilesUploadEnded())
                .subscribe(progressInfo -> view.onMediaFilesUploadProgress(progressInfo), throwable -> {
                    Timber.d(throwable);
                    Crashlytics.logException(throwable);
                })
        );
    }

    @Override
    public void stopUploading() {
        disposables.clear();
    }

    @Override
    public void destroy() {
        disposables.dispose();
        cacheWordDataSource.dispose();
        view = null;
    }
}
