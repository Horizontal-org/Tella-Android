package rs.readahead.washington.mobile.mvp.presenter;

import org.javarosa.core.model.FormDef;

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
import rs.readahead.washington.mobile.domain.exception.NoConnectivityException;
import rs.readahead.washington.mobile.domain.repository.IOpenRosaRepository;
import rs.readahead.washington.mobile.mvp.contract.ICollectBlankFormListPresenterContract;
import timber.log.Timber;


public class CollectBlankFormListPresenter implements
        ICollectBlankFormListPresenterContract.IPresenter {
    private IOpenRosaRepository odkRepository;
    private ICollectBlankFormListPresenterContract.IView view;
    private CompositeDisposable disposables = new CompositeDisposable();
    private KeyDataSource keyDataSource;


    public CollectBlankFormListPresenter(ICollectBlankFormListPresenterContract.IView view) {
        this.odkRepository = new OpenRosaRepository();
        this.view = view;
        this.keyDataSource = MyApplication.getKeyDataSource();
    }

    @Override
    public void refreshBlankForms() {
        disposables.add(keyDataSource.getDataSource()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe(disposable -> view.showBlankFormRefreshLoading())
                .flatMap((Function<DataSource, ObservableSource<List<CollectServer>>>) dataSource ->
                        dataSource.listCollectServers().toObservable())
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
                        listFormResult -> keyDataSource.getDataSource().flatMap((Function<DataSource, ObservableSource<ListFormResult>>)
                                dataSource -> dataSource.updateBlankForms(listFormResult).toObservable()))
                .doFinally(() -> view.hideBlankFormRefreshLoading())
                .subscribe(listFormResult -> {
                    // log errors if any in result..
                    for (IErrorBundle error : listFormResult.getErrors()) {
                        Timber.e(error.getException());
                    }

                    view.onBlankFormsListResult(listFormResult);
                }, throwable -> {
                    if (throwable instanceof NoConnectivityException) {
                        view.onNoConnectionAvailable();
                    } else {
                        Timber.e(throwable);
                        view.onBlankFormsListError(throwable);
                    }
                })
        );
    }

    @Override
    public void listBlankForms() {
        disposables.add(keyDataSource.getDataSource()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .flatMap((Function<DataSource, ObservableSource<List<CollectForm>>>)
                        dataSource -> dataSource.listBlankForms().toObservable())
                .subscribe(forms -> view.onBlankFormsListResult(new ListFormResult(forms)),
                        throwable -> view.onBlankFormsListError(throwable))
        );
    }

    @Override
    public void removeBlankFormDef(CollectForm form) {
        disposables.add(keyDataSource.getDataSource()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .flatMapCompletable(dataSource -> dataSource.removeBlankFormDef(form))
                .subscribe(() -> view.onBlankFormDefRemoved(),
                        throwable -> {
                            Timber.e(throwable);
                            view.onBlankFormDefRemoveError(throwable);
                        })
        );
    }

    @Override
    public void downloadBlankFormDef(final CollectForm form) {
        disposables.add(keyDataSource.getDataSource()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe(disposable -> view.onDownloadBlankFormDefStart())
                .flatMap((Function<DataSource, ObservableSource<CollectServer>>) dataSource ->
                        dataSource.getCollectServer(form.getServerId()).toObservable()
                ).flatMap((Function<CollectServer, ObservableSource<FormDef>>) server ->
                        odkRepository.getFormDef(server, form).toObservable()
                ).flatMap((Function<FormDef, ObservableSource<FormDef>>) formDef ->
                        keyDataSource.getDataSource().flatMap((Function<DataSource, ObservableSource<FormDef>>) dataSource ->
                                dataSource.updateBlankFormDef(form, formDef).toObservable()
                        )
                )
                .doFinally(() -> view.onDownloadBlankFormDefEnd())
                .subscribe(
                        formDef -> view.onDownloadBlankFormDefSuccess(form),
                        throwable -> {
                            Timber.e(throwable);
                            view.onFormDefError(throwable);
                        }
                )
        );
    }

    @Override
    public void updateBlankFormDef(final CollectForm form) {
        disposables.add(keyDataSource.getDataSource()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe(disposable -> view.onUpdateBlankFormDefStart())
                .flatMap((Function<DataSource, ObservableSource<CollectServer>>) dataSource ->
                        dataSource.getCollectServer(form.getServerId()).toObservable()
                ).flatMap((Function<CollectServer, ObservableSource<FormDef>>) server ->
                        odkRepository.getFormDef(server, form).toObservable()
                ).flatMap((Function<FormDef, ObservableSource<FormDef>>) formDef ->
                        keyDataSource.getDataSource().flatMap((Function<DataSource, ObservableSource<FormDef>>) dataSource ->
                                dataSource.updateBlankCollectFormDef(form, formDef).toObservable()
                        )
                )
                .doFinally(() -> view.onUpdateBlankFormDefEnd())
                .subscribe(
                        formDef -> view.onUpdateBlankFormDefSuccess(form, formDef),
                        throwable -> {
                            Timber.e(throwable);
                            view.onFormDefError(throwable);
                        }
                )
        );
    }

    public void userCancel() {
        disposables.clear();
        view.onUserCancel();
    }

    @Override
    public void destroy() {
        disposables.dispose();
        view = null;
    }
}