package rs.readahead.washington.mobile.mvp.presenter;

import com.crashlytics.android.Crashlytics;

import java.util.ArrayList;
import java.util.List;

import info.guardianproject.cacheword.CacheWordHandler;
import info.guardianproject.cacheword.ICacheWordSubscriber;
import io.reactivex.ObservableSource;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.AsyncSubject;
import rs.readahead.washington.mobile.MyApplication;
import rs.readahead.washington.mobile.data.database.DataSource;
import rs.readahead.washington.mobile.data.repository.OpenRosaRepository;
import rs.readahead.washington.mobile.domain.entity.IErrorBundle;
import rs.readahead.washington.mobile.domain.entity.collect.CollectForm;
import rs.readahead.washington.mobile.domain.entity.collect.CollectServer;
import rs.readahead.washington.mobile.domain.entity.collect.ListFormResult;
import rs.readahead.washington.mobile.domain.exception.NoConnectivityException;
import rs.readahead.washington.mobile.domain.repository.IOpenRosaRepository;
import rs.readahead.washington.mobile.mvp.contract.ICollectBlankFormListPresenterContract;


public class CollectBlankFormListPresenter implements
        ICollectBlankFormListPresenterContract.IPresenter,
        ICacheWordSubscriber {
    private IOpenRosaRepository odkRepository;
    private ICollectBlankFormListPresenterContract.IView view;
    private CacheWordHandler cacheWordHandler;
    private CompositeDisposable disposables = new CompositeDisposable();
    private AsyncSubject<DataSource> asyncDataSource = AsyncSubject.create();


    public CollectBlankFormListPresenter(ICollectBlankFormListPresenterContract.IView view) {
        this.odkRepository = new OpenRosaRepository();
        this.view = view;
        this.cacheWordHandler = new CacheWordHandler(view.getContext().getApplicationContext(), this);

        cacheWordHandler.connectToService();
    }

    @Override
    public void refreshBlankForms() {
        disposables.add(asyncDataSource
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe(disposable -> view.showBlankFormRefreshLoading())
                .flatMap((Function<DataSource, ObservableSource<List<CollectServer>>>) dataSource -> dataSource.listCollectServers().toObservable())
                .flatMap((Function<List<CollectServer>, ObservableSource<ListFormResult>>) servers -> {
                    if (servers.isEmpty()) {
                        return Single.just(new ListFormResult()).toObservable();
                    }

                    if (!MyApplication.isConnectedToInternet(view.getContext())) {
                        throw new NoConnectivityException();
                    }

                    List<Single<ListFormResult>> singles = new ArrayList<>();
                    for (CollectServer server : servers) {
                        singles.add(odkRepository.formList(server));
                    }

                    // result and errors are wrapped - no error should be thrown => for zip to work..
                    return Single.zip(singles, objects -> {
                        ListFormResult allResults = new ListFormResult();

                        for (Object obj : objects) {
                            if (obj instanceof ListFormResult) {
                                @SuppressWarnings("unchecked")
                                List<CollectForm> forms = ((ListFormResult) obj).getForms();
                                List<IErrorBundle> errors = ((ListFormResult) obj).getErrors();

                                allResults.getForms().addAll(forms);
                                allResults.getErrors().addAll(errors);
                            }
                        }

                        return allResults;
                    }).toObservable();
                })
                .flatMap((Function<ListFormResult, ObservableSource<ListFormResult>>)
                        listFormResult -> asyncDataSource.flatMap((Function<DataSource, ObservableSource<ListFormResult>>)
                                dataSource -> dataSource.updateBlankForms(listFormResult).toObservable()))
                .doFinally(() -> view.hideBlankFormRefreshLoading())
                .subscribe(listFormResult -> {
                    // log errors if any in result..
                    for (IErrorBundle error : listFormResult.getErrors()) {
                        Crashlytics.logException(error.getException());
                    }

                    view.onBlankFormsListResult(listFormResult);
                }, throwable -> {
                    if (throwable instanceof NoConnectivityException) {
                        view.onNoConnectionAvailable();
                    } else {
                        Crashlytics.logException(throwable);
                        view.onBlankFormsListError(throwable);
                    }
                })
        );
    }


    @Override
    public void listBlankForms() {
        disposables.add(asyncDataSource
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .flatMap((Function<DataSource, ObservableSource<List<CollectForm>>>)
                        dataSource -> dataSource.listBlankForms().toObservable())
                .subscribe(forms -> view.onBlankFormsListResult(new ListFormResult(forms)),
                        throwable -> view.onBlankFormsListError(throwable))
        );
    }

    @Override
    public void destroy() {
        if (cacheWordHandler != null) {
            cacheWordHandler.disconnectFromService();
        }
        disposables.dispose();
        view = null;
    }

    @Override
    public void onCacheWordUninitialized() {
    }

    @Override
    public void onCacheWordLocked() {
    }

    @Override
    public void onCacheWordOpened() {
        if (view != null) {
            DataSource dataSource = DataSource.getInstance(view.getContext(), cacheWordHandler.getEncryptionKey());
            asyncDataSource.onNext(dataSource);
            asyncDataSource.onComplete();
        }

        cacheWordHandler.disconnectFromService();
        cacheWordHandler = null;
    }
}
