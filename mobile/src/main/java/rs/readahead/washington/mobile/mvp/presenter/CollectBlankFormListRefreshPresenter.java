package rs.readahead.washington.mobile.mvp.presenter;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.ObservableSource;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import rs.readahead.washington.mobile.MyApplication;
import rs.readahead.washington.mobile.data.database.DataSource;
import rs.readahead.washington.mobile.data.database.KeyDataSource;
import rs.readahead.washington.mobile.data.repository.OpenRosaRepository;
import rs.readahead.washington.mobile.domain.entity.IErrorBundle;
import rs.readahead.washington.mobile.domain.entity.collect.CollectForm;
import rs.readahead.washington.mobile.domain.entity.collect.CollectServer;
import rs.readahead.washington.mobile.domain.entity.collect.ListFormResult;
import rs.readahead.washington.mobile.domain.repository.IOpenRosaRepository;
import rs.readahead.washington.mobile.mvp.contract.ICollectBlankFormListRefreshPresenterContract;
import timber.log.Timber;


public class CollectBlankFormListRefreshPresenter implements
        ICollectBlankFormListRefreshPresenterContract.IPresenter {
    private final IOpenRosaRepository odkRepository;
    private ICollectBlankFormListRefreshPresenterContract.IView view;
    private final CompositeDisposable disposables = new CompositeDisposable();
    private final KeyDataSource keyDataSource;


    public CollectBlankFormListRefreshPresenter(ICollectBlankFormListRefreshPresenterContract.IView view) {
        this.odkRepository = new OpenRosaRepository();
        this.view = view;
        this.keyDataSource = MyApplication.getKeyDataSource();
    }

    @Override
    public void refreshBlankForms() {
        disposables.add(keyDataSource.getDataSource()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .flatMap((Function<DataSource, ObservableSource<List<CollectServer>>>) dataSource ->
                        dataSource.listCollectServers().toObservable())
                .flatMap((Function<List<CollectServer>, ObservableSource<ListFormResult>>) servers -> {
                    if (servers.isEmpty()) {
                        return Single.just(new ListFormResult()).toObservable();
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
                        listFormResult -> keyDataSource.getDataSource().flatMap((Function<DataSource, ObservableSource<ListFormResult>>)
                                dataSource -> dataSource.updateBlankForms(listFormResult).toObservable()))
                .subscribe(listFormResult -> {
                    // log errors if any in result..
                    for (IErrorBundle error : listFormResult.getErrors()) {
                        Timber.e(error.getException());
                    }
                }, throwable -> {
                    Timber.e(throwable);
                    view.onRefreshBlankFormsError(throwable);
                })
        );
    }

    @Override
    public void destroy() {
        disposables.dispose();
        view = null;
    }
}
