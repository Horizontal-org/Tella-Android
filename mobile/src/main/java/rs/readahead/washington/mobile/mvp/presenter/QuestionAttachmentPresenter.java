package rs.readahead.washington.mobile.mvp.presenter;

import android.net.Uri;
import androidx.annotation.Nullable;

import com.crashlytics.android.Crashlytics;

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
import rs.readahead.washington.mobile.mvp.contract.IQuestionAttachmentPresenterContract;


public class QuestionAttachmentPresenter implements IQuestionAttachmentPresenterContract.IPresenter {
    private IQuestionAttachmentPresenterContract.IView view;
    private CompositeDisposable disposables = new CompositeDisposable();
    private CacheWordDataSource cacheWordDataSource;
    private MediaFileHandler mediaFileHandler;

    @Nullable
    private MediaFile attachment;


    public QuestionAttachmentPresenter(IQuestionAttachmentPresenterContract.IView view) {
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
                        .subscribe(mediaFiles -> view.onGetFilesSuccess(mediaFiles), throwable -> view.onGetFilesError(throwable))
        );
    }

    @Override
    public void setAttachment(@Nullable MediaFile attachment) {
        this.attachment = attachment;
    }

    @Nullable
    @Override
    public MediaFile getAttachment() {
        return attachment;
    }

    @Override
    public void addNewMediaFile(MediaFileBundle mediaFileBundle) {
        disposables.add(mediaFileHandler.registerMediaFile(mediaFileBundle)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(mediaFile -> view.onMediaFileAdded(attachment), throwable -> view.onMediaFileAddError(throwable))
        );
    }

    @Override
    public void addRegisteredMediaFile(final long id) {
        disposables.add(cacheWordDataSource.getDataSource()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .flatMapSingle((Function<DataSource, SingleSource<MediaFile>>) dataSource -> dataSource.getMediaFile(id))
                .subscribe(mediaFile -> view.onMediaFileAdded(attachment), throwable -> view.onMediaFileAddError(throwable))
        );
    }

    @Override
    public void importImage(final Uri uri) {
        disposables.add(Observable.fromCallable(() -> MediaFileHandler.importPhotoUri(view.getContext(), uri))
                .subscribeOn(Schedulers.computation())
                .doOnSubscribe(disposable -> view.onImportStarted())
                .observeOn(AndroidSchedulers.mainThread())
                .doFinally(() -> view.onImportEnded())
                .subscribe(mediaHolder -> view.onMediaFileImported(mediaHolder), throwable -> {
                    Crashlytics.logException(throwable);
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
                .subscribe(mediaHolder -> view.onMediaFileImported(mediaHolder), throwable -> {
                    Crashlytics.logException(throwable);
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
