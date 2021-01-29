package rs.readahead.washington.mobile.mvp.presenter;

import org.hzontal.tella.keys.key.LifecycleMainKey;

import java.util.List;

import io.reactivex.SingleSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.AsyncSubject;
import rs.readahead.washington.mobile.MyApplication;
import rs.readahead.washington.mobile.data.database.DataSource;
import rs.readahead.washington.mobile.domain.entity.collect.CollectFormInstance;
import rs.readahead.washington.mobile.mvp.contract.ICollectFormInstanceListPresenterContract;


public class CollectFormInstanceListPresenter implements
        ICollectFormInstanceListPresenterContract.IPresenter {
    private ICollectFormInstanceListPresenterContract.IView view;
    private CompositeDisposable disposables = new CompositeDisposable();
    private AsyncSubject<DataSource> asyncDataSource = AsyncSubject.create();


    public CollectFormInstanceListPresenter(ICollectFormInstanceListPresenterContract.IView view) {
        this.view = view;
        initDataSource();
    }

    @Override
    public void listDraftFormInstances() {
        disposables.add(asyncDataSource
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
        disposables.add(asyncDataSource
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .flatMapSingle((Function<DataSource, SingleSource<List<CollectFormInstance>>>) DataSource::listSentForms)
                .subscribe(forms -> view.onFormInstanceListSuccess(forms),
                        throwable -> view.onFormInstanceListError(throwable)
                )
        );
    }

    private void initDataSource() {
        if (view != null) {
            DataSource dataSource;
            try {
                dataSource = DataSource.getInstance(view.getContext(), MyApplication.getMainKeyHolder().get().getKey().getEncoded());
                asyncDataSource.onNext(dataSource);
                asyncDataSource.onComplete();
            } catch (LifecycleMainKey.MainKeyUnavailableException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void destroy() {
        disposables.dispose();
        view = null;
    }

}
