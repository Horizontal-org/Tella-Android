package rs.readahead.washington.mobile.mvp.presenter;

import com.crashlytics.android.Crashlytics;

import io.reactivex.SingleSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import rs.readahead.washington.mobile.data.database.CacheWordDataSource;
import rs.readahead.washington.mobile.data.database.DataSource;
import rs.readahead.washington.mobile.domain.entity.collect.CollectFormInstance;
import rs.readahead.washington.mobile.mvp.contract.IFormSubmitPresenterContract;


public class FormSubmitPresenter implements IFormSubmitPresenterContract.IPresenter {
    private IFormSubmitPresenterContract.IView view;
    private CompositeDisposable disposables = new CompositeDisposable();
    private CacheWordDataSource cacheWordDataSource;


    public FormSubmitPresenter(IFormSubmitPresenterContract.IView view) {
        this.view = view;
        this.cacheWordDataSource = new CacheWordDataSource(view.getContext().getApplicationContext());
    }

    @Override
    public void getFormInstance(long instanceId) {
        disposables.add(cacheWordDataSource.getDataSource()
                .flatMapSingle((Function<DataSource, SingleSource<CollectFormInstance>>) dataSource ->
                        dataSource.getInstance(instanceId))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(instance -> view.onGetFormInstanceSuccess(instance), throwable -> {
                    Crashlytics.logException(throwable);
                    view.onGetFormInstanceError(throwable);
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
