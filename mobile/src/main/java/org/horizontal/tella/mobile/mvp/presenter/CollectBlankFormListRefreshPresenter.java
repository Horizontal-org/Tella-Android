package org.horizontal.tella.mobile.mvp.presenter;

import com.google.firebase.crashlytics.FirebaseCrashlytics;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.ObservableSource;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import org.horizontal.tella.mobile.MyApplication;
import org.horizontal.tella.mobile.data.database.DataSource;
import org.horizontal.tella.mobile.data.database.KeyDataSource;
import org.horizontal.tella.mobile.data.repository.OpenRosaRepository;
import org.horizontal.tella.mobile.domain.entity.IErrorBundle;
import org.horizontal.tella.mobile.domain.entity.collect.CollectForm;
import org.horizontal.tella.mobile.domain.entity.collect.CollectServer;
import org.horizontal.tella.mobile.domain.entity.collect.ListFormResult;
import org.horizontal.tella.mobile.domain.repository.IOpenRosaRepository;
import org.horizontal.tella.mobile.mvp.contract.ICollectBlankFormListRefreshPresenterContract;


public class CollectBlankFormListRefreshPresenter implements
        ICollectBlankFormListRefreshPresenterContract.IPresenter {
    private IOpenRosaRepository odkRepository;
    private ICollectBlankFormListRefreshPresenterContract.IView view;
    private CompositeDisposable disposables = new CompositeDisposable();
    private KeyDataSource keyDataSource;


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
                        listFormResult -> keyDataSource.getDataSource().flatMap((Function<DataSource, ObservableSource<ListFormResult>>)
                                dataSource -> dataSource.updateBlankForms(listFormResult).toObservable()))
                .subscribe(listFormResult -> {
                    // log errors if any in result..
                    for (IErrorBundle error : listFormResult.getErrors()) {
                        FirebaseCrashlytics.getInstance().recordException(error.getException());
                    }
                }, throwable -> {
                    FirebaseCrashlytics.getInstance().recordException(throwable);
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
