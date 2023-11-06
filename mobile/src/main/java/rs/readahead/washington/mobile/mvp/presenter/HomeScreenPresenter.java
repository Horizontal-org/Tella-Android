package rs.readahead.washington.mobile.mvp.presenter;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInstaller;
import android.net.Uri;

import com.google.firebase.crashlytics.FirebaseCrashlytics;

import io.reactivex.Completable;
import io.reactivex.SingleSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import rs.readahead.washington.mobile.BuildConfig;
import rs.readahead.washington.mobile.MyApplication;
import rs.readahead.washington.mobile.data.database.DataSource;
import rs.readahead.washington.mobile.data.database.KeyDataSource;
import rs.readahead.washington.mobile.data.database.UwaziDataSource;
import rs.readahead.washington.mobile.data.sharedpref.Preferences;
import rs.readahead.washington.mobile.data.sharedpref.SharedPrefs;
import rs.readahead.washington.mobile.media.MediaFileHandler;
import rs.readahead.washington.mobile.mvp.contract.IHomeScreenPresenterContract;


public class HomeScreenPresenter implements IHomeScreenPresenterContract.IPresenter {
    private final Context appContext;
    private IHomeScreenPresenterContract.IView view;
    private CompositeDisposable disposable;
    private final KeyDataSource keyDataSource;


    public HomeScreenPresenter(IHomeScreenPresenterContract.IView view) {
        this.view = view;
        appContext = view.getContext().getApplicationContext();
        keyDataSource = MyApplication.getKeyDataSource();
        disposable = new CompositeDisposable();
    }

    @Override
    public void executePanicMode() {
        keyDataSource.getDataSource()
                .subscribeOn(Schedulers.io())
                .flatMapCompletable(dataSource -> {
                    if (SharedPrefs.getInstance().isEraseGalleryActive()) {
                        MyApplication.rxVault.destroy().blockingAwait();
                        MediaFileHandler.destroyGallery(appContext);
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
                    MyApplication.resetKeys();
                    if (Preferences.isUninstallOnPanic()) {
                        uninstallTella(view.getContext());
                    }

                    return Completable.complete();
                })
                .blockingAwait();
    }

    @Override
    public void countTUServers() {
        disposable.add(keyDataSource.getDataSource()
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

    @Override
    public void countCollectServers() {
        disposable.add(keyDataSource.getDataSource()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .flatMapSingle((Function<DataSource, SingleSource<Long>>) DataSource::countCollectServers)
                .subscribe(
                        num -> view.onCountCollectServersEnded(num),
                        throwable -> {
                            FirebaseCrashlytics.getInstance().recordException(throwable);
                            view.onCountCollectServersFailed(throwable);
                        }
                )
        );
    }

    @Override
    public void countUwaziServers() {
        disposable.add(keyDataSource.getUwaziDataSource()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .flatMapSingle((Function<UwaziDataSource, SingleSource<Long>>) UwaziDataSource::countUwaziServers)
                .subscribe(
                        num -> view.onCountUwaziServersEnded(num),
                        throwable -> {
                            FirebaseCrashlytics.getInstance().recordException(throwable);
                            view.onCountUwaziServersFailed(throwable);
                        }
                )
        );
    }

    @Override
    public void destroy() {
        disposable.dispose();
        view = null;
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