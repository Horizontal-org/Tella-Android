package org.horizontal.tella.mobile.mvp.presenter;

import com.google.firebase.crashlytics.FirebaseCrashlytics;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import org.horizontal.tella.mobile.MyApplication;
import org.horizontal.tella.mobile.data.database.DataSource;
import org.horizontal.tella.mobile.data.database.KeyDataSource;
import org.horizontal.tella.mobile.data.sharedpref.Preferences;
import org.horizontal.tella.mobile.mvp.contract.IServersPresenterContract;


public class ServersPresenter implements IServersPresenterContract.IPresenter {
    private KeyDataSource keyDataSource;
    private IServersPresenterContract.IView view;
    private CompositeDisposable disposables = new CompositeDisposable();


    public ServersPresenter(IServersPresenterContract.IView view) {
        keyDataSource = MyApplication.getKeyDataSource();
        this.view = view;
    }

    @Override
    public void deleteServers() {
        disposables.add(keyDataSource.getDataSource()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .flatMapCompletable(DataSource::deleteAllServers)
                .subscribe(
                        () -> view.onServersDeleted(),
                        throwable -> {
                            FirebaseCrashlytics.getInstance().recordException(throwable);
                            view.onServersDeletedError(throwable);
                        }
                )
        );
    }

    @Override
    public void removeAutoUploadServersSettings() {
        Preferences.setAutoUpload(false);
        Preferences.setAutoUploadServerId(-1);
    }

    @Override
    public long getAutoUploadServerId() {
        return Preferences.getAutoUploadServerId();
    }

    @Override
    public void setAutoUploadServerId(long id) {
        Preferences.setAutoUploadServerId(id);
    }

    @Override
    public void destroy() {
        disposables.dispose();
        view = null;
    }
}
