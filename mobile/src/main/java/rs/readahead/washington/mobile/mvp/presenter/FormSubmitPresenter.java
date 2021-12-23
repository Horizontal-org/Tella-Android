package rs.readahead.washington.mobile.mvp.presenter;

import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.hzontal.tella_vault.rx.RxVault;

import io.reactivex.ObservableSource;
import io.reactivex.SingleSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import rs.readahead.washington.mobile.MyApplication;
import rs.readahead.washington.mobile.data.KeyRxVault;
import rs.readahead.washington.mobile.data.database.DataSource;
import rs.readahead.washington.mobile.data.database.KeyDataSource;
import rs.readahead.washington.mobile.domain.entity.collect.CollectFormInstance;
import rs.readahead.washington.mobile.mvp.contract.IFormSubmitPresenterContract;

public class FormSubmitPresenter implements IFormSubmitPresenterContract.IPresenter {
    private IFormSubmitPresenterContract.IView view;
    private final CompositeDisposable disposables = new CompositeDisposable();
    private final KeyDataSource keyDataSource;
    private final KeyRxVault keyRxVault;

    public FormSubmitPresenter(IFormSubmitPresenterContract.IView view) {
        this.view = view;
        this.keyDataSource = MyApplication.getKeyDataSource();
        this.keyRxVault = MyApplication.getKeyRxVault();
    }

    @Override
    public void getFormInstance(long instanceId) {
        disposables.add(keyDataSource.getDataSource()
                .flatMapSingle((Function<DataSource, SingleSource<CollectFormInstance>>) dataSource ->
                        dataSource.getInstance(instanceId))
                .flatMap(instance ->
                        keyRxVault.getRxVault().flatMap((Function<RxVault, ObservableSource<CollectFormInstance>>)
                                instance::loadWidgetMediaFiles))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(instance -> view.onGetFormInstanceSuccess(instance), throwable -> {
                    FirebaseCrashlytics.getInstance().recordException(throwable);
                    view.onGetFormInstanceError(throwable);
                })
        );
    }

    @Override
    public void destroy() {
        disposables.dispose();
        view = null;
    }
}
