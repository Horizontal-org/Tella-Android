package rs.readahead.washington.mobile.mvp.presenter;

import android.content.Context;
import android.net.Uri;

import com.google.firebase.crashlytics.FirebaseCrashlytics;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.SingleSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import rs.readahead.washington.mobile.data.database.CacheWordDataSource;
import rs.readahead.washington.mobile.data.database.DataSource;
import rs.readahead.washington.mobile.domain.entity.MediaFile;
import rs.readahead.washington.mobile.domain.repository.IMediaFileRecordRepository;
import rs.readahead.washington.mobile.media.MediaFileHandler;
import rs.readahead.washington.mobile.mvp.contract.IGalleryPresenterContract;
import rs.readahead.washington.mobile.presentation.entity.MediaFileThumbnailData;


public class GalleryPresenter implements IGalleryPresenterContract.IPresenter {
    private IGalleryPresenterContract.IView view;
    private CompositeDisposable disposables = new CompositeDisposable();
    private CacheWordDataSource cacheWordDataSource;
    private MediaFileHandler mediaFileHandler;


    public GalleryPresenter(IGalleryPresenterContract.IView view) {
        this.view = view;
        this.cacheWordDataSource = new CacheWordDataSource(view.getContext());
        this.mediaFileHandler = new MediaFileHandler(cacheWordDataSource);
    }

    @Override
    public void getFiles(final IMediaFileRecordRepository.Filter filter, final IMediaFileRecordRepository.Sort sort) {
        disposables.add(cacheWordDataSource.getDataSource()
                .flatMapSingle((Function<DataSource, SingleSource<List<MediaFile>>>) dataSource ->
                        dataSource.listMediaFiles(filter, sort))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe(disposable -> view.onGetFilesStart())
                .doFinally(() -> view.onGetFilesEnd())
                .subscribe(mediaFiles -> {
                    //checkMediaFolder(view.getContext(), mediaFiles);
                    view.onGetFilesSuccess(mediaFiles);
                }, throwable -> {
                    FirebaseCrashlytics.getInstance().recordException(throwable);
                    view.onGetFilesError(throwable);
                })
        );
    }

    @Override
    public void destroy() {
        disposables.dispose();
        cacheWordDataSource.dispose();
        view = null;
    }

    @Override
    public void importImage(final Uri uri) {
        disposables.add(Observable
                .fromCallable(() -> MediaFileHandler.importPhotoUri(view.getContext(), uri))
                .subscribeOn(Schedulers.computation())
                .doOnSubscribe(disposable -> view.onImportStarted())
                .observeOn(AndroidSchedulers.mainThread())
                .doFinally(() -> view.onImportEnded())
                .subscribe(mediaHolder ->
                            view.onMediaImported(mediaHolder.getMediaFile(), mediaHolder.getMediaFileThumbnailData()),
                        throwable -> {
                            FirebaseCrashlytics.getInstance().recordException(throwable);
                            view.onImportError(throwable);
                        })
        );
    }

    @Override
    public void importVideo(final Uri uri) {
        disposables.add(Observable
                .fromCallable(() -> MediaFileHandler.importVideoUri(view.getContext(), uri))
                .subscribeOn(Schedulers.computation())
                .doOnSubscribe(disposable -> view.onImportStarted())
                .observeOn(AndroidSchedulers.mainThread())
                .doFinally(() -> view.onImportEnded())
                .subscribe(mediaHolder -> view.onMediaImported(mediaHolder.getMediaFile(), mediaHolder.getMediaFileThumbnailData()), throwable -> {
                    FirebaseCrashlytics.getInstance().recordException(throwable);
                    view.onImportError(throwable);
                })
        );
    }

    @Override
    public void addNewMediaFile(MediaFile mediaFile, MediaFileThumbnailData thumbnailData) {
        disposables.add(mediaFileHandler.registerMediaFile(mediaFile, thumbnailData)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(mediaFile1 -> view.onMediaFilesAdded(mediaFile1), throwable -> {
                    FirebaseCrashlytics.getInstance().recordException(throwable);
                    view.onMediaFilesAddingError(throwable);
                })
        );
    }

    @Override
    public void deleteMediaFiles(final List<MediaFile> mediaFiles) {
        disposables.add(cacheWordDataSource.getDataSource()
                .subscribeOn(Schedulers.io())
                .flatMapSingle((Function<DataSource, SingleSource<Integer>>) dataSource -> {
                    List<Single<MediaFile>> completables = new ArrayList<>();
                    for (MediaFile mediafile: mediaFiles) {
                        completables.add(dataSource.deleteMediaFile(mediafile, mediaFile ->
                                MediaFileHandler.deleteMediaFile(view.getContext(), mediaFile)));
                    }

                    return Single.zip(completables, objects -> objects.length);
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(num -> view.onMediaFilesDeleted(num), throwable -> {
                    FirebaseCrashlytics.getInstance().recordException(throwable);
                    view.onMediaFilesDeletionError(throwable);
                })
        );
    }

    @Override
    public void exportMediaFiles(final List<MediaFile> mediaFiles) {
        final Context context = view.getContext().getApplicationContext();

        disposables.add(Single
                .fromCallable(() -> {
                    for (MediaFile mediaFile: mediaFiles) {
                        MediaFileHandler.exportMediaFile(context, mediaFile);
                    }

                    return mediaFiles.size();
                })
                .subscribeOn(Schedulers.computation())
                .doOnSubscribe(disposable -> view.onExportStarted())
                .observeOn(AndroidSchedulers.mainThread())
                .doFinally(() -> view.onExportEnded())
                .subscribe(num -> view.onMediaExported(num), throwable -> {
                    FirebaseCrashlytics.getInstance().recordException(throwable);
                    view.onExportError(throwable);
                })
        );
    }

    @Override
    public void countTUServers() {
        disposables.add(cacheWordDataSource.getDataSource()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .flatMapSingle((Function<DataSource, SingleSource<Long>>) DataSource::countTUServers)
                .subscribe(
                        num -> view.onCountTUServersEnded(num),
                        throwable -> {
                            FirebaseCrashlytics.getInstance().recordException(throwable);
                            view.onCountTUServersFailed(throwable);
                        }
                )
        );
    }
}
