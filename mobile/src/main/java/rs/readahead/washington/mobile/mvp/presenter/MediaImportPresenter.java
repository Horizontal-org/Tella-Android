package rs.readahead.washington.mobile.mvp.presenter;

import android.net.Uri;

import androidx.annotation.Nullable;

import com.hzontal.tella_vault.VaultFile;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import rs.readahead.washington.mobile.media.MediaFileHandler;
import rs.readahead.washington.mobile.mvp.contract.IMediaImportPresenterContract;
import timber.log.Timber;


public class MediaImportPresenter implements IMediaImportPresenterContract.IPresenter {
    private IMediaImportPresenterContract.IView view;
    private final CompositeDisposable disposables = new CompositeDisposable();

    @Nullable
    private VaultFile attachment;

    public MediaImportPresenter(IMediaImportPresenterContract.IView view) {
        this.view = view;
    }

    @Override
    public void importImage(final Uri uri) {
        disposables.add(Observable.fromCallable(() -> MediaFileHandler.importPhotoUri(view.getContext(), uri,null))
                .subscribeOn(Schedulers.computation())
                .doOnSubscribe(disposable -> view.onImportStarted())
                .observeOn(AndroidSchedulers.mainThread())
                .doFinally(() -> view.onImportEnded())
                .subscribe(vaultFile -> view.onMediaFileImported(vaultFile), throwable -> {
                    Timber.e(throwable);//TODO Crahslytics removed
                    view.onImportError(throwable);
                })
        );
    }

    @Override
    public void importVideo(final Uri uri) {
        disposables.add(Observable.fromCallable(() -> MediaFileHandler.importVideoUri(view.getContext(), uri,null))
                .subscribeOn(Schedulers.computation())
                .doOnSubscribe(disposable -> view.onImportStarted())
                .observeOn(AndroidSchedulers.mainThread())
                .doFinally(() -> view.onImportEnded())
                .subscribe(mediaHolder -> view.onMediaFileImported(mediaHolder), throwable -> {
                    Timber.e(throwable);//TODO Crahslytics removed
                    view.onImportError(throwable);
                })
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
        view.onMediaFileImported(attachment);
    }

    @Override
    public void addRegisteredMediaFile(final long id) {
    }

    @Override
    public void importFile(final Uri uri) {
        disposables.add(Observable.fromCallable(() -> MediaFileHandler.importVaultFileUri(view.getContext(), uri,null))
                .subscribeOn(Schedulers.computation())
                .doOnSubscribe(disposable -> view.onImportStarted())
                .observeOn(AndroidSchedulers.mainThread())
                .doFinally(() -> view.onImportEnded())
                .subscribe(vaultFile -> view.onMediaFileImported(vaultFile), throwable -> {
                    Timber.e(throwable);//TODO Crahslytics removed
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
