package rs.readahead.washington.mobile.mvp.presenter;

import com.google.firebase.crashlytics.FirebaseCrashlytics;

import java.util.List;

import io.reactivex.SingleSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import rs.readahead.washington.mobile.MyApplication;
import rs.readahead.washington.mobile.data.database.DataSource;
import rs.readahead.washington.mobile.data.database.KeyDataSource;
import rs.readahead.washington.mobile.data.openrosa.OpenRosaService;
import rs.readahead.washington.mobile.domain.entity.reports.TellaReportServer;
import rs.readahead.washington.mobile.mvp.contract.ITellaUploadServersPresenterContract;


public class TellaUploadServersPresenter implements ITellaUploadServersPresenterContract.IPresenter {
    private final KeyDataSource keyDataSource;
    private ITellaUploadServersPresenterContract.IView view;
    private final CompositeDisposable disposables = new CompositeDisposable();


    //@Inject
    public TellaUploadServersPresenter(ITellaUploadServersPresenterContract.IView view) {
        keyDataSource = MyApplication.getKeyDataSource();
        this.view = view;
    }

    public void getTUServers() {
        disposables.add(keyDataSource.getDataSource()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe(disposable -> view.showLoading())
                .flatMapSingle((Function<DataSource, SingleSource<List<TellaReportServer>>>)
                        DataSource::listTellaUploadServers)
                .doFinally(() -> view.hideLoading())
                .subscribe(list -> view.onTUServersLoaded(list),
                        throwable -> {
                            FirebaseCrashlytics.getInstance().recordException(throwable);
                            view.onLoadTUServersError(throwable);
                        })
        );
    }

    public void create(final TellaReportServer server) {
        disposables.add(keyDataSource.getDataSource()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe(disposable -> view.showLoading())
                .flatMapSingle((Function<DataSource, SingleSource<TellaReportServer>>)
                        dataSource -> dataSource.createTellaUploadServer(server))
                .doFinally(() -> view.hideLoading())
                .subscribe(server1 -> view.onCreatedTUServer(server1),
                        throwable -> {
                            FirebaseCrashlytics.getInstance().recordException(throwable);
                            view.onCreateTUServerError(throwable);
                        })
        );
    }

    public void update(final TellaReportServer server) {
        disposables.add(keyDataSource.getDataSource()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe(disposable -> view.showLoading())
                .flatMapSingle((Function<DataSource, SingleSource<TellaReportServer>>)
                        dataSource -> dataSource.updateTellaUploadServer(server))
                .doFinally(() -> view.hideLoading())
                .subscribe(server1 -> {
                            OpenRosaService.clearCache();
                            view.onUpdatedTUServer(server1);
                        },
                        throwable -> {
                            FirebaseCrashlytics.getInstance().recordException(throwable);
                            view.onUpdateTUServerError(throwable);
                        })
        );
    }

    public void remove(final TellaReportServer server) {
        disposables.add(keyDataSource.getDataSource()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe(disposable -> view.showLoading())
                .flatMapCompletable(dataSource -> dataSource.removeTUServer(server.getId()))
                .doFinally(() -> view.hideLoading())
                .subscribe(() -> {
                            OpenRosaService.clearCache();
                            view.onRemovedTUServer(server);
                        },
                        throwable -> {
                            FirebaseCrashlytics.getInstance().recordException(throwable);
                            view.onRemoveTUServerError(throwable);
                        })
        );
    }

    @Override
    public void destroy() {
        disposables.dispose();
        view = null;
    }
}
