package rs.readahead.washington.mobile.mvp.presenter;

import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.hzontal.tella_vault.VaultFile;

import java.util.concurrent.Callable;

import io.reactivex.Completable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import rs.readahead.washington.mobile.MyApplication;
import rs.readahead.washington.mobile.data.database.KeyDataSource;
import rs.readahead.washington.mobile.media.MediaFileHandler;
import rs.readahead.washington.mobile.mvp.contract.IMediaFileViewerPresenterContract;


public class MediaFileViewerPresenter implements IMediaFileViewerPresenterContract.IPresenter {
    private IMediaFileViewerPresenterContract.IView view;
    private CompositeDisposable disposables = new CompositeDisposable();
    private KeyDataSource keyDataSource;


    public MediaFileViewerPresenter(IMediaFileViewerPresenterContract.IView view) {
        this.view = view;
        this.keyDataSource = MyApplication.getKeyDataSource();
    }

    @Override
    public void exportNewMediaFile(final VaultFile vaultFile) {
        disposables.add(Completable.fromCallable((Callable<Void>) () -> {
                    MediaFileHandler.exportMediaFile(view.getContext().getApplicationContext(), vaultFile);
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
        MyApplication.rxVault.delete(vaultFile)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe( v -> view.onMediaFileDeleted(), throwable -> {
                    FirebaseCrashlytics.getInstance().recordException(throwable);
                    view.onMediaFileDeletionError(throwable);
                }).dispose();
    }

    @Override
    public void destroy() {
        disposables.dispose();
        view = null;
    }
}
