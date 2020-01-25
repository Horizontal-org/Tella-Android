package rs.readahead.washington.mobile.mvp.presenter;

import com.crashlytics.android.Crashlytics;

import java.util.List;

import io.reactivex.SingleSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import rs.readahead.washington.mobile.data.database.CacheWordDataSource;
import rs.readahead.washington.mobile.data.database.DataSource;
import rs.readahead.washington.mobile.domain.entity.TellaUploadServer;
import rs.readahead.washington.mobile.mvp.contract.ITellaUploadDialogPresenterContract;


public class TellaUploadDialogPresenter implements ITellaUploadDialogPresenterContract.IPresenter {
    private CacheWordDataSource cacheWordDataSource;
    private ITellaUploadDialogPresenterContract.IView view;
    private CompositeDisposable disposables = new CompositeDisposable();


    public TellaUploadDialogPresenter(ITellaUploadDialogPresenterContract.IView view) {
        cacheWordDataSource = new CacheWordDataSource(view.getContext().getApplicationContext());
        this.view = view;
    }

    public void loadServers() {
        disposables.add(cacheWordDataSource.getDataSource()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .flatMapSingle((Function<DataSource, SingleSource<List<TellaUploadServer>>>)
                        DataSource::listTellaUploadServers)
                .subscribe(list -> view.onServersLoaded(list),
                        throwable -> {
                            Crashlytics.logException(throwable);
                            view.onServersLoadError(throwable);
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
