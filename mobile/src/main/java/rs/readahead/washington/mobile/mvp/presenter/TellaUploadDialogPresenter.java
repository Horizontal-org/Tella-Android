package rs.readahead.washington.mobile.mvp.presenter;

import java.util.List;

import io.reactivex.SingleSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import rs.readahead.washington.mobile.MyApplication;
import rs.readahead.washington.mobile.data.database.DataSource;
import rs.readahead.washington.mobile.data.database.KeyDataSource;
import rs.readahead.washington.mobile.domain.entity.TellaUploadServer;
import rs.readahead.washington.mobile.mvp.contract.ITellaUploadDialogPresenterContract;
import timber.log.Timber;


public class TellaUploadDialogPresenter implements ITellaUploadDialogPresenterContract.IPresenter {
    private KeyDataSource keyDataSource;
    private ITellaUploadDialogPresenterContract.IView view;
    private CompositeDisposable disposables = new CompositeDisposable();


    public TellaUploadDialogPresenter(ITellaUploadDialogPresenterContract.IView view) {
        keyDataSource = MyApplication.getKeyDataSource();
        this.view = view;
    }

    public void loadServers() {
        disposables.add(keyDataSource.getDataSource()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .flatMapSingle((Function<DataSource, SingleSource<List<TellaUploadServer>>>)
                        DataSource::listTellaUploadServers)
                .subscribe(list -> view.onServersLoaded(list),
                        throwable -> {
                            Timber.e(throwable);
                            view.onServersLoadError(throwable);
                        })
        );
    }

    @Override
    public void destroy() {
        disposables.dispose();
        view = null;
    }
}
