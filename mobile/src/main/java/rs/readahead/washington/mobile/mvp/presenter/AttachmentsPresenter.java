package rs.readahead.washington.mobile.mvp.presenter;

import android.annotation.SuppressLint;
import android.net.Uri;

import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.hzontal.tella_vault.VaultFile;

import java.util.ArrayList;
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
import rs.readahead.washington.mobile.mvp.contract.IAttachmentsPresenterContract;


public class AttachmentsPresenter implements IAttachmentsPresenterContract.IPresenter {
    private IAttachmentsPresenterContract.IView view;
    private CompositeDisposable disposables = new CompositeDisposable();
    private KeyDataSource keyDataSource;
    private MediaFileHandler mediaFileHandler;

    private List<VaultFile> attachments = new ArrayList<VaultFile>();


    public AttachmentsPresenter(IAttachmentsPresenterContract.IView view) {
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
                        .subscribe(mediaFiles -> {
                            //checkMediaFolder(view.getContext(), mediaFiles);
                            view.onGetFilesSuccess(mediaFiles);
                        }, throwable -> view.onGetFilesError(throwable))
        );
    }

    @Override
    public List<VaultFile> getAttachments() {
        return attachments;
    }

    @Override
    public void setAttachments(List<VaultFile> attachments) {
        this.attachments = attachments;
    }

    @SuppressLint("CheckResult")
    @Override
    public void attachNewEvidence(VaultFile vaultFile) {
        //TODO check vault file creation
        MyApplication.rxVault.builder("")
                .setDuration(vaultFile.duration)
                .setAnonymous(vaultFile.anonymous)
                .setThumb(vaultFile.thumb)
                .setMimeType(vaultFile.mimeType)
                .setMetadata(vaultFile.metadata)
                .setParent(MyApplication.rxVault.getRoot().blockingGet())
                .setName(vaultFile.name)
                .setId(vaultFile.id)
                .setType(vaultFile.type)
                .build()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(mediaFile -> {
                    if (!attachments.contains(mediaFile)) {
                        attachments.add(mediaFile);
                        view.onEvidenceAttached(mediaFile);
                    }
                }, throwable -> view.onEvidenceAttachedError(throwable))
                .dispose();
    }

    @Override
    public void attachRegisteredEvidence(final String id) {
        MyApplication.rxVault.get(id)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(mediaFile -> {
                    if (!attachments.contains(mediaFile)) {
                        attachments.add(mediaFile);
                        view.onEvidenceAttached(mediaFile);
                    }
                }, throwable -> view.onEvidenceAttachedError(throwable))
                .dispose();
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
        view = null;
    }
}
