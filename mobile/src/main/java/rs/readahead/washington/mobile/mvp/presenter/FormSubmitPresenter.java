package rs.readahead.washington.mobile.mvp.presenter;

import com.hzontal.tella_vault.VaultFile;

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
import rs.readahead.washington.mobile.domain.entity.collect.FormMediaFile;
import rs.readahead.washington.mobile.domain.entity.collect.FormMediaFileStatus;
import rs.readahead.washington.mobile.mvp.contract.IFormSubmitPresenterContract;
import timber.log.Timber;


public class FormSubmitPresenter implements IFormSubmitPresenterContract.IPresenter {
    private IFormSubmitPresenterContract.IView view;
    private final CompositeDisposable disposables = new CompositeDisposable();
    private final KeyDataSource keyDataSource;
    private CollectFormInstance collectFormInstance;

    public FormSubmitPresenter(IFormSubmitPresenterContract.IView view) {
        this.view = view;
        this.keyDataSource = MyApplication.getKeyDataSource();
    }

    @Override
    public void getFormInstance(long instanceId) {
        disposables.add(keyDataSource.getDataSource()
                .flatMapSingle((Function<DataSource, SingleSource<CollectFormInstance>>) dataSource ->
                        dataSource.getInstance(instanceId))
                .flatMapSingle((Function<CollectFormInstance, SingleSource<List<VaultFile>>>) instance -> {
                    collectFormInstance = instance;
                    return MyApplication.rxVault.get(instance.getWidgetMediaFilesIds());
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(vaultFiles -> {
                    collectFormInstance.setCollectInstanceAttachments(vaultFiles);
                    view.onGetFormInstanceSuccess(collectFormInstance);
                }, throwable -> {
                    Timber.e(throwable);//TODO Crahslytics removed
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
