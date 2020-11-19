
package rs.readahead.washington.mobile.mvp.presenter;

import android.net.Uri;

import com.google.firebase.crashlytics.FirebaseCrashlytics;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.SingleSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import rs.readahead.washington.mobile.data.database.CacheWordDataSource;
import rs.readahead.washington.mobile.data.database.DataSource;
import rs.readahead.washington.mobile.domain.entity.MediaFile;
import rs.readahead.washington.mobile.domain.repository.IMediaFileRecordRepository;
import rs.readahead.washington.mobile.media.MediaFileBundle;
import rs.readahead.washington.mobile.media.MediaFileHandler;
import rs.readahead.washington.mobile.mvp.contract.IAttachmentsPresenterContract;


public class AttachmentsPresenter implements IAttachmentsPresenterContract.IPresenter {
    private IAttachmentsPresenterContract.IView view;
    private CompositeDisposable disposables = new CompositeDisposable();
    private CacheWordDataSource cacheWordDataSource;
    private MediaFileHandler mediaFileHandler;

    private List<MediaFile> attachments = new ArrayList<>();


    public AttachmentsPresenter(IAttachmentsPresenterContract.IView view) {
        this.view = view;
        this.cacheWordDataSource = new CacheWordDataSource(view.getContext());
        this.mediaFileHandler = new MediaFileHandler(cacheWordDataSource);
    }

    @Override
    public void getFiles(final IMediaFileRecordRepository.Filter filter, final IMediaFileRecordRepository.Sort sort) {
        disposables.add(
                cacheWordDataSource.getDataSource()
                        .flatMapSingle((Function<DataSource, SingleSource<List<MediaFile>>>) dataSource -> dataSource.listMediaFiles(filter, sort))
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .doOnSubscribe(disposable -> view.onGetFilesStart())
                        .doFinally(() -> view.onGetFilesEnd())
                        .subscribe(mediaFiles -> {
                            //checkMediaFolder(view.getContext(), mediaFiles);
                            view.onGetFilesSuccess(mediaFiles);
                        }, throwable -> view.onGetFilesError(throwable))
        );
    }

    @Override
    public void setAttachments(List<MediaFile> attachments) {
        this.attachments = attachments;
    }

    @Override
    public List<MediaFile> getAttachments() {
        return attachments;
    }

    @Override
    public void attachNewEvidence(MediaFileBundle mediaFileBundle) {
        disposables.add(mediaFileHandler.registerMediaFile(mediaFileBundle)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(mediaFile -> {
                    if (!attachments.contains(mediaFile)) {
                        attachments.add(mediaFile);
                        view.onEvidenceAttached(mediaFile);
                    }
                }, throwable -> view.onEvidenceAttachedError(throwable))
        );
    }

    @Override
    public void attachRegisteredEvidence(final long id) {
        disposables.add(cacheWordDataSource.getDataSource()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .flatMapSingle((Function<DataSource, SingleSource<MediaFile>>) dataSource -> dataSource.getMediaFile(id))
                .subscribe(mediaFile -> {
                    if (!attachments.contains(mediaFile)) {
                        attachments.add(mediaFile);
                        view.onEvidenceAttached(mediaFile);
                    }
                }, throwable -> view.onEvidenceAttachedError(throwable))
        );
    }

    @Override
    public void importImage(final Uri uri) {
        disposables.add(Observable.fromCallable(() -> MediaFileHandler.importPhotoUri(view.getContext(), uri))
                .subscribeOn(Schedulers.computation())
                .doOnSubscribe(disposable -> view.onImportStarted())
                .observeOn(AndroidSchedulers.mainThread())
                .doFinally(() -> view.onImportEnded())
                .subscribe(mediaHolder -> view.onEvidenceImported(mediaHolder), throwable -> {
                    FirebaseCrashlytics.getInstance().recordException(throwable);
                    view.onImportError(throwable);
                })
        );
    }


    @Override
    public void importVideo(final Uri uri) {
        disposables.add(Observable.fromCallable(() -> MediaFileHandler.importVideoUri(view.getContext(), uri))
                .subscribeOn(Schedulers.computation())
                .doOnSubscribe(disposable -> view.onImportStarted())
                .observeOn(AndroidSchedulers.mainThread())
                .doFinally(() -> view.onImportEnded())
                .subscribe(mediaHolder -> view.onEvidenceImported(mediaHolder), throwable -> {
                    FirebaseCrashlytics.getInstance().recordException(throwable);
                    view.onImportError(throwable);
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
