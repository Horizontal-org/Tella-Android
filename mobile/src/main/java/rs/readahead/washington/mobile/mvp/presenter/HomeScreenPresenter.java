package rs.readahead.washington.mobile.mvp.presenter;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import com.crashlytics.android.Crashlytics;

import info.guardianproject.cacheword.CacheWordHandler;
import io.reactivex.Completable;
import io.reactivex.SingleSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import rs.readahead.washington.mobile.BuildConfig;
import rs.readahead.washington.mobile.MyApplication;
import rs.readahead.washington.mobile.data.database.CacheWordDataSource;
import rs.readahead.washington.mobile.data.database.DataSource;
import rs.readahead.washington.mobile.data.sharedpref.Preferences;
import rs.readahead.washington.mobile.data.sharedpref.SharedPrefs;
import rs.readahead.washington.mobile.media.MediaFileHandler;
import rs.readahead.washington.mobile.mvp.contract.IHomeScreenPresenterContract;


public class HomeScreenPresenter implements IHomeScreenPresenterContract.IPresenter {
    private IHomeScreenPresenterContract.IView view;
    private CacheWordHandler cacheWordHandler;
    private CacheWordDataSource cacheWordDataSource;
    private CompositeDisposable disposable;
    private final Context appContext;


    public HomeScreenPresenter(IHomeScreenPresenterContract.IView view, CacheWordHandler cacheWordHandler) {
        this.view = view;
        this.cacheWordHandler = cacheWordHandler;
        appContext = view.getContext().getApplicationContext();
        cacheWordDataSource = new CacheWordDataSource(appContext);
        disposable = new CompositeDisposable();
    }

    @Override
    public void executePanicMode() {
        cacheWordDataSource.getDataSource()
                .subscribeOn(Schedulers.io())
                .flatMapCompletable(dataSource -> {
                    if (SharedPrefs.getInstance().isEraseGalleryActive()) {
                        MediaFileHandler.destroyGallery(appContext);
                        dataSource.deleteMediaFiles();
                    }

                    if (Preferences.isDeleteServerSettingsActive()) {
                        dataSource.deleteDatabase();
                    } else {

                        if (Preferences.isEraseForms()) {
                            dataSource.deleteForms();
                        }

                    }

                    clearSharedPreferences();

                    MyApplication.exit(view.getContext());

                    lockCacheWord();

                    if (Preferences.isUninstallOnPanic()) {
                        uninstallTella(view.getContext());
                    }

                    return Completable.complete();
                })
                .blockingAwait();
    }

    @Override
    public void countTUServers() {
        disposable.add(cacheWordDataSource.getDataSource()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .flatMapSingle((Function<DataSource, SingleSource<Long>>) DataSource::countTUServers)
                .subscribe(
                        num -> view.onCountTUServersEnded(num),
                        throwable -> {
                            Crashlytics.logException(throwable);
                            view.onCountTUServersFailed(throwable);
                        }
                )
        );
    }

    @Override
    public void countCollectServers() {
        disposable.add(cacheWordDataSource.getDataSource()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .flatMapSingle((Function<DataSource, SingleSource<Long>>) DataSource::countCollectServers)
                .subscribe(
                        num -> view.onCountCollectServersEnded(num),
                        throwable -> {
                            Crashlytics.logException(throwable);
                            view.onCountCollectServersFailed(throwable);
                        }
                )
        );
    }

    @Override
    public void destroy() {
        disposable.dispose();
        cacheWordDataSource.dispose();
        view = null;
        cacheWordHandler = null;
    }

    private void lockCacheWord() {
        if (cacheWordHandler != null && !cacheWordHandler.isLocked()) {
            cacheWordHandler.lock();
        }
    }

    private void clearSharedPreferences() {
        Preferences.setPanicMessage(null);
    }

    private void uninstallTella(Context context) {
        Uri packageUri = Uri.parse("package:" + BuildConfig.APPLICATION_ID);
        Intent intent = new Intent(Intent.ACTION_UNINSTALL_PACKAGE, packageUri);
        context.startActivity(intent);
    }
}