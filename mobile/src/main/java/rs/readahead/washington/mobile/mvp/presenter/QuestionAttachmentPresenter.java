package rs.readahead.washington.mobile.mvp.presenter;

import android.net.Uri;

import androidx.annotation.Nullable;

import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.hzontal.tella_vault.VaultFile;

import java.util.List;

import io.reactivex.Observable;
import io.reactivex.SingleSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import rs.readahead.washington.mobile.MyApplication;
import rs.readahead.washington.mobile.data.database.DataSource;
import rs.readahead.washington.mobile.data.database.KeyDataSource;
import rs.readahead.washington.mobile.domain.repository.IMediaFileRecordRepository;
import rs.readahead.washington.mobile.media.MediaFileHandler;
import rs.readahead.washington.mobile.mvp.contract.IQuestionAttachmentPresenterContract;


public class QuestionAttachmentPresenter implements IQuestionAttachmentPresenterContract.IPresenter {
    private IQuestionAttachmentPresenterContract.IView view;
    private CompositeDisposable disposables = new CompositeDisposable();
    private final KeyDataSource keyDataSource;
    private final MediaFileHandler mediaFileHandler;

    @Nullable
    private VaultFile attachment;


    public QuestionAttachmentPresenter(IQuestionAttachmentPresenterContract.IView view) {
        this.view = view;
        this.keyDataSource = MyApplication.getKeyDataSource();
        this.mediaFileHandler = new MediaFileHandler(keyDataSource);
    }

    @Override
    public void getFiles(final IMediaFileRecordRepository.Filter filter, final IMediaFileRecordRepository.Sort sort) {
        disposables.add(
                keyDataSource.getDataSource()
                        .flatMapSingle((Function<DataSource, SingleSource<List<VaultFile>>>) dataSource -> dataSource.listMediaFiles(filter, sort))
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .doOnSubscribe(disposable -> view.onGetFilesStart())
                        .doFinally(() -> view.onGetFilesEnd())
                        .subscribe(mediaFiles -> view.onGetFilesSuccess(mediaFiles), throwable -> view.onGetFilesError(throwable))
        );
    }

    @Nullable
    @Override
    public VaultFile getAttachment() {
        return attachment;
    }

    @Override
    public void setAttachment(@Nullable VaultFile attachment) {
        this.attachment = attachment;
    }

    @Override
    public void addNewMediaFile(VaultFile vaultFile) {
        disposables.add(mediaFileHandler.registerMediaFile(vaultFile)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(mediaFile -> view.onMediaFileAdded(attachment), throwable -> view.onMediaFileAddError(throwable))
        );
    }

    @Override
    public void addRegisteredMediaFile(final long id) {
        disposables.add(keyDataSource.getDataSource()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .flatMapSingle((Function<DataSource, SingleSource<VaultFile>>) dataSource -> dataSource.getMediaFile(id))
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
                .subscribe(mediaHolder -> view.onMediaFileImported(mediaHolder), throwable -> {
                    FirebaseCrashlytics.getInstance().recordException(throwable);
                    view.onImportError(throwable);
                })
        );
    }

    @Override
    public void destroy() {
        disposables.dispose();
        view = null;
    }
}
