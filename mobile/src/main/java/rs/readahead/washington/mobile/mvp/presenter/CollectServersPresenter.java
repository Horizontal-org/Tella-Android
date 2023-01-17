package rs.readahead.washington.mobile.mvp.presenter;

//import com.google.firebase.crashlytics.FirebaseCrashlytics;

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
import rs.readahead.washington.mobile.domain.entity.collect.CollectServer;
import rs.readahead.washington.mobile.mvp.contract.ICollectServersPresenterContract;
import timber.log.Timber;


public class CollectServersPresenter implements ICollectServersPresenterContract.IPresenter {
    private KeyDataSource keyDataSource;
    private ICollectServersPresenterContract.IView view;
    private CompositeDisposable disposables = new CompositeDisposable();

    //@Inject
    public CollectServersPresenter(ICollectServersPresenterContract.IView view) {
        keyDataSource = MyApplication.getKeyDataSource();
        this.view = view;
    }

    public void getCollectServers() {
        disposables.add(keyDataSource.getDataSource()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe(disposable -> view.showLoading())
                .flatMapSingle((Function<DataSource, SingleSource<List<CollectServer>>>)
                        DataSource::listCollectServers)
                .doFinally(() -> view.hideLoading())
                .subscribe(list -> view.onServersLoaded(list),
                        throwable -> {
                            Timber.e(throwable);
                            view.onLoadServersError(throwable);
                        })
        );
    }

    public void create(final CollectServer server) {
        disposables.add(keyDataSource.getDataSource()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe(disposable -> view.showLoading())
                .flatMapSingle((Function<DataSource, SingleSource<CollectServer>>)
                        dataSource -> dataSource.createCollectServer(server))
                .doFinally(() -> view.hideLoading())
                .subscribe(server1 -> view.onCreatedServer(server1),
                        throwable -> {
                            Timber.e(throwable);
                            view.onCreateCollectServerError(throwable);
                        })
        );
    }

    public void update(final CollectServer server) {
        disposables.add(keyDataSource.getDataSource()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe(disposable -> view.showLoading())
                .flatMapSingle((Function<DataSource, SingleSource<CollectServer>>)
                        dataSource -> dataSource.updateCollectServer(server))
                .doFinally(() -> view.hideLoading())
                .subscribe(server1 -> {
                            OpenRosaService.clearCache();
                            view.onUpdatedServer(server1);
                        },
                        throwable -> {
                            Timber.e(throwable);
                            view.onUpdateServerError(throwable);
                        })
        );
    }

    public void remove(final CollectServer server) {
        disposables.add(keyDataSource.getDataSource()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe(disposable -> view.showLoading())
                .flatMapCompletable(dataSource -> dataSource.removeCollectServer(server.getId()))
                .doFinally(() -> view.hideLoading())
                .subscribe(() -> {
                            OpenRosaService.clearCache();
                            view.onRemovedServer(server);
                        },
                        throwable -> {
                            Timber.e(throwable);
                            view.onRemoveServerError(throwable);
                        })
        );
    }

    @Override
    public void destroy() {
        disposables.dispose();
        view = null;
    }
}
