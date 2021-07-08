package rs.readahead.washington.mobile.mvp.presenter;

import android.content.Context;
import android.net.Uri;

import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.hzontal.tella_vault.IVaultDatabase;
import com.hzontal.tella_vault.Vault;
import com.hzontal.tella_vault.VaultFile;
import com.hzontal.tella_vault.rx.RxVault;

import java.util.ArrayList;
import java.util.List;


import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.Scheduler;
import io.reactivex.Single;
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
import rs.readahead.washington.mobile.mvp.contract.IGalleryPresenterContract;


public class GalleryPresenter implements IGalleryPresenterContract.IPresenter {
    private IGalleryPresenterContract.IView view;
    private CompositeDisposable disposables = new CompositeDisposable();
    private KeyDataSource keyDataSource;
    private MediaFileHandler mediaFileHandler;


    public GalleryPresenter(IGalleryPresenterContract.IView view) {
        this.view = view;
        this.keyDataSource = MyApplication.getKeyDataSource();
        this.mediaFileHandler = new MediaFileHandler(keyDataSource);
    }

    @Override
    public void getFiles(final IVaultDatabase.Filter filter, final IVaultDatabase.Sort sort) {

        disposables.add(Single
                .fromCallable(() ->  MyApplication.rxVault.list(null,filter,sort,null))
                .subscribeOn(Schedulers.io())
                .doOnSubscribe(disposable -> view.onGetFilesStart())
                .observeOn(AndroidSchedulers.mainThread())
                .doFinally(() -> view.onGetFilesEnd())
                .subscribe(vaultFile -> view.onGetFilesSuccess(vaultFile.blockingGet()),throwable ->  {FirebaseCrashlytics.getInstance().recordException(throwable);
                    view.onGetFilesError(throwable);
                }));
    }

    @Override
    public void destroy() {
        disposables.dispose();
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
                .subscribe(vaultFile -> view.onMediaImported(vaultFile),throwable ->  {FirebaseCrashlytics.getInstance().recordException(throwable);
                    view.onImportError(throwable);
                }));

    }

    @Override
    public void importVideo(final Uri uri) {
        disposables.add(Observable
                .fromCallable(() -> MediaFileHandler.importVideoUri(view.getContext(), uri))
                .subscribeOn(Schedulers.computation())
                .doOnSubscribe(disposable -> view.onImportStarted())
                .observeOn(AndroidSchedulers.mainThread())
                .doFinally(() -> view.onImportEnded())
                .subscribe(vaultFile -> view.onMediaImported(vaultFile), throwable -> {
                    FirebaseCrashlytics.getInstance().recordException(throwable);
                    view.onImportError(throwable);
                })
        );
    }

    @Override
    public void addNewMediaFile(VaultFile vaultFile) {
        disposables.add(mediaFileHandler.saveVaultFile(vaultFile)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(vaultFile1 -> view.onMediaFilesAdded(vaultFile1), throwable -> {
                    FirebaseCrashlytics.getInstance().recordException(throwable);
                    view.onMediaFilesAddingError(throwable);
                })
        );
    }

    @Override
    public void deleteMediaFiles(final List<VaultFile> vaultFiles) {

        List<Single<Boolean>> completables = new ArrayList<>();
        for (VaultFile vaultFile : vaultFiles) {
            completables.add(deleteMediaFile(vaultFile));
        }
        disposables.add(
         Single.zip(completables, objects -> objects.length)
                 .observeOn(AndroidSchedulers.mainThread())
                 .subscribe(num -> view.onMediaFilesDeleted(num), throwable -> {
                     FirebaseCrashlytics.getInstance().recordException(throwable);
                     view.onMediaFilesDeletionError(throwable);
                 }));

    }

    private Single<Boolean> deleteMediaFile(VaultFile vaultFile){
        return MyApplication.rxVault.delete(vaultFile)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    @Override
    public void exportMediaFiles(final List<VaultFile> vaultFiles) {
        final Context context = view.getContext().getApplicationContext();

        disposables.add(Single
                .fromCallable(() -> {
                    for (VaultFile mediaFile : vaultFiles) {
                        MediaFileHandler.exportMediaFile(context, mediaFile);
                    }

                    return vaultFiles.size();
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
        disposables.add(keyDataSource.getDataSource()
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
