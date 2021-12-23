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
import rs.readahead.washington.mobile.domain.entity.collect.CollectFormInstance;
import rs.readahead.washington.mobile.mvp.contract.ICollectFormInstanceListPresenterContract;

public class CollectFormInstanceListPresenter implements
        ICollectFormInstanceListPresenterContract.IPresenter {
    private ICollectFormInstanceListPresenterContract.IView view;
    private final CompositeDisposable disposables = new CompositeDisposable();
    private final KeyDataSource keyDataSource;


    public CollectFormInstanceListPresenter(ICollectFormInstanceListPresenterContract.IView view) {
        this.view = view;
        this.keyDataSource = MyApplication.getKeyDataSource();
    }

    @Override
    public void listDraftFormInstances() {
        disposables.add(keyDataSource.getDataSource()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .flatMapSingle((Function<DataSource, SingleSource<List<CollectFormInstance>>>) DataSource::listDraftForms)
                .subscribe(forms -> view.onFormInstanceListSuccess(forms),
                        throwable -> view.onFormInstanceListError(throwable)
                )
        );
    }

    @Override
    public void listSubmitFormInstances() {
        disposables.add(keyDataSource.getDataSource()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .flatMapSingle((Function<DataSource, SingleSource<List<CollectFormInstance>>>) DataSource::listSentForms)
                .subscribe(forms -> view.onFormInstanceListSuccess(forms),
                        throwable -> view.onFormInstanceListError(throwable)
                )
        );
    }

    @Override
    public void listOutboxFormInstances() {
        disposables.add(keyDataSource.getDataSource()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .flatMapSingle((Function<DataSource, SingleSource<List<CollectFormInstance>>>) DataSource::listPendingForms)
                .subscribe(forms -> view.onFormInstanceListSuccess(forms),
                        throwable -> view.onFormInstanceListError(throwable)
                )
        );
    }

    @Override
    public void deleteFormInstance(final CollectFormInstance instance) {
        disposables.add(keyDataSource.getDataSource()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .flatMapCompletable(dataSource -> dataSource.deleteInstance(instance.getId()))
                .subscribe(
                        () -> view.onFormInstanceDeleteSuccess(instance.getInstanceName()),
                        throwable -> {
                            FirebaseCrashlytics.getInstance().recordException(throwable);
                            view.onFormInstanceDeleteError(throwable);
                        }
                )
        );
    }

    @Override
    public void destroy() {
        disposables.dispose();
        view = null;
    }
}
