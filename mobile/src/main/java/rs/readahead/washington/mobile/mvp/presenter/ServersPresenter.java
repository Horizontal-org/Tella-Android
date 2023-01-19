package rs.readahead.washington.mobile.mvp.presenter;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import rs.readahead.washington.mobile.MyApplication;
import rs.readahead.washington.mobile.data.database.DataSource;
import rs.readahead.washington.mobile.data.database.KeyDataSource;
import rs.readahead.washington.mobile.data.sharedpref.Preferences;
import rs.readahead.washington.mobile.mvp.contract.IServersPresenterContract;
import timber.log.Timber;


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
                            Timber.e(throwable);
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
