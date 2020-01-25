package rs.readahead.washington.mobile.mvp.presenter;

import com.crashlytics.android.Crashlytics;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import rs.readahead.washington.mobile.data.database.CacheWordDataSource;
import rs.readahead.washington.mobile.data.database.DataSource;
import rs.readahead.washington.mobile.mvp.contract.IServersPresenterContract;


public class ServersPresenter implements IServersPresenterContract.IPresenter {
    private CacheWordDataSource cacheWordDataSource;
    private IServersPresenterContract.IView view;
    private CompositeDisposable disposables = new CompositeDisposable();


    public ServersPresenter(IServersPresenterContract.IView view) {
        cacheWordDataSource = new CacheWordDataSource(view.getContext().getApplicationContext());
        this.view = view;
    }

    @Override
    public void deleteServers() {
        disposables.add(cacheWordDataSource.getDataSource()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .flatMapCompletable(DataSource::deleteAllServers)
                .subscribe(
                        () -> view.onServersDeleted(),
                        throwable -> {
                            Crashlytics.logException(throwable);
                            view.onServersDeletedError(throwable);
                        }
                )
        );
    }

    @Override
    public void destroy() {
        disposables.dispose();
        cacheWordDataSource.dispose();
        view = null;
    }
}
