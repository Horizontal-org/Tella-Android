package rs.readahead.washington.mobile.mvp.presenter;

import android.net.Uri;

import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.hzontal.tella_vault.VaultFile;

import java.util.List;
import java.util.concurrent.Callable;

import io.reactivex.Completable;
import io.reactivex.SingleSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import rs.readahead.washington.mobile.MyApplication;
import rs.readahead.washington.mobile.data.database.DataSource;
import rs.readahead.washington.mobile.data.database.KeyDataSource;
import rs.readahead.washington.mobile.domain.entity.collect.FormMediaFile;
import rs.readahead.washington.mobile.media.MediaFileHandler;
import rs.readahead.washington.mobile.mvp.contract.IMediaFileViewerPresenterContract;


public class MediaFileViewerPresenter implements IMediaFileViewerPresenterContract.IPresenter {
    private final CompositeDisposable disposables = new CompositeDisposable();
    private IMediaFileViewerPresenterContract.IView view;
    private KeyDataSource keyDataSource;

    public MediaFileViewerPresenter(IMediaFileViewerPresenterContract.IView view) {
        this.view = view;
        keyDataSource = MyApplication.getKeyDataSource();
    }

    @Override
    public void exportNewMediaFile(Boolean withMetadata, final VaultFile vaultFile, Uri path) {
        disposables.add(Completable.fromCallable((Callable<Void>) () -> {
                    MediaFileHandler.exportMediaFile(view.getContext().getApplicationContext(), vaultFile, path);
                    if (withMetadata && vaultFile.metadata != null) {
                        MediaFileHandler.exportMediaFile(view.getContext().getApplicationContext(), MediaFileHandler.maybeCreateMetadataMediaFile(vaultFile), path);
                    }
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
    public void deleteMediaFiles(final VaultFile vaultFile) {
        disposables.add(MyApplication.rxVault.delete(vaultFile)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(isDeleted -> view.onMediaFileDeleted(), throwable -> {
                    FirebaseCrashlytics.getInstance().recordException(throwable);
                    view.onMediaFileDeletionError(throwable);
                }));
    }

    @Override
    public void confirmDeleteMediaFile(VaultFile vaultFile) {
        disposables.add(keyDataSource.getDataSource()
                .flatMapSingle((Function<DataSource, SingleSource<List<FormMediaFile>>>) DataSource::getReportMediaFiles)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(formMediaFileList -> {
                    if (formMediaFileList.isEmpty()) {
                        view.onMediaFileDeleteConfirmation(vaultFile, false);
                    } else {
                        boolean isShowConfirmation = false;
                        //#1. Iterate through list
                        for (FormMediaFile formMediaFile : formMediaFileList) {
                            if (formMediaFile.id.equals(vaultFile.id)) {
                                isShowConfirmation = true;
                                break;
                            }
                        }
                        view.onMediaFileDeleteConfirmation(vaultFile, isShowConfirmation);
                    }
                }, throwable -> {
                    FirebaseCrashlytics.getInstance().recordException(throwable);
                    view.onMediaFileDeletionError(throwable);
                })
        );
    }

    @Override
    public void renameVaultFile(String id, String name) {
        disposables.add(MyApplication.rxVault.rename(id, name)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(vaultFile -> view.onMediaFileRename(vaultFile), throwable -> {
                    FirebaseCrashlytics.getInstance().recordException(throwable);
                    view.onMediaFileRenameError(throwable);
                }));
    }

    @Override
    public void destroy() {
        disposables.dispose();
        view = null;
    }
}
