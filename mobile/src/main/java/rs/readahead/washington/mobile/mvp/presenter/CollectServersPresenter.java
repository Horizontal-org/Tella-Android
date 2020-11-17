package rs.readahead.washington.mobile.mvp.presenter;

import com.google.firebase.crashlytics.FirebaseCrashlytics;

import java.util.List;

import io.reactivex.SingleSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import rs.readahead.washington.mobile.data.database.CacheWordDataSource;
import rs.readahead.washington.mobile.data.database.DataSource;
import rs.readahead.washington.mobile.data.openrosa.OpenRosaService;
import rs.readahead.washington.mobile.domain.entity.collect.CollectServer;
import rs.readahead.washington.mobile.mvp.contract.ICollectServersPresenterContract;


public class CollectServersPresenter implements ICollectServersPresenterContract.IPresenter {
    private CacheWordDataSource cacheWordDataSource;
    private ICollectServersPresenterContract.IView view;
    private CompositeDisposable disposables = new CompositeDisposable();


    //@Inject
    public CollectServersPresenter(ICollectServersPresenterContract.IView view) {
        cacheWordDataSource = new CacheWordDataSource(view.getContext().getApplicationContext());
        this.view = view;
    }

    public void getCollectServers() {
        disposables.add(cacheWordDataSource.getDataSource()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe(disposable -> view.showLoading())
                .flatMapSingle((Function<DataSource, SingleSource<List<CollectServer>>>)
                        DataSource::listCollectServers)
                .doFinally(() -> view.hideLoading())
                .subscribe(list -> view.onServersLoaded(list),
                        throwable -> {
                            FirebaseCrashlytics.getInstance().recordException(throwable);
                            view.onLoadServersError(throwable);
                        })
        );
    }

    public void create(final CollectServer server) {
        disposables.add(cacheWordDataSource.getDataSource()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe(disposable -> view.showLoading())
                .flatMapSingle((Function<DataSource, SingleSource<CollectServer>>)
                        dataSource -> dataSource.createCollectServer(server))
                .doFinally(() -> view.hideLoading())
                .subscribe(server1 -> view.onCreatedServer(server1),
                        throwable -> {
                            FirebaseCrashlytics.getInstance().recordException(throwable);
                            view.onCreateCollectServerError(throwable);
                        })
        );
    }

    public void update(final CollectServer server) {
        disposables.add(cacheWordDataSource.getDataSource()
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
                            FirebaseCrashlytics.getInstance().recordException(throwable);
                            view.onUpdateServerError(throwable);
                        })
        );
    }

    public void remove(final CollectServer server) {
        disposables.add(cacheWordDataSource.getDataSource()
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
                            FirebaseCrashlytics.getInstance().recordException(throwable);
                            view.onRemoveServerError(throwable);
                        })
        );
    }

    @Override
    public void destroy() {
        disposables.dispose();
        cacheWordDataSource.dispose();
        view = null;
    }
}
