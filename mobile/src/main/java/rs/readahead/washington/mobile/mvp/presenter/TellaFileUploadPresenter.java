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
import rs.readahead.washington.mobile.domain.entity.FileUploadInstance;
import rs.readahead.washington.mobile.domain.repository.ITellaUploadsRepository;
import rs.readahead.washington.mobile.mvp.contract.ITellaFileUploadPresenterContract;

public class TellaFileUploadPresenter implements ITellaFileUploadPresenterContract.IPresenter {
    private ITellaFileUploadPresenterContract.IView view;
    private CompositeDisposable disposables = new CompositeDisposable();
    private CacheWordDataSource cacheWordDataSource;


    public TellaFileUploadPresenter(ITellaFileUploadPresenterContract.IView view) {
        this.view = view;
        this.cacheWordDataSource = new CacheWordDataSource(view.getContext());
    }

    @Override
    public void destroy() {
        disposables.dispose();
        cacheWordDataSource.dispose();
        view = null;
    }

    @Override
    public void getFileUploadInstances()  {
        disposables.add(cacheWordDataSource.getDataSource()
                .flatMapSingle((Function<DataSource, SingleSource<List<FileUploadInstance>>>) DataSource::getFileUploadInstances)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                //.doOnSubscribe(disposable -> view.())
                //.doFinally(() -> view.())
                .subscribe(filesUploadInstances -> view.onGetFileUploadInstancesSuccess(filesUploadInstances), throwable -> {
                    Crashlytics.logException(throwable);
                    view.onGetFileUploadInstancesError(throwable);
                })
        );
    }

    @Override
    public void deleteFileUploadInstances(ITellaUploadsRepository.UploadStatus status) {
        disposables.add(cacheWordDataSource.getDataSource()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .flatMapCompletable(dataSource -> dataSource.deleteFileUploadInstances(status))
                .subscribe(() -> view.onFileUploadInstancesDeleted(),
                        throwable -> {
                            Crashlytics.logException(throwable);
                            view.onFileUploadInstancesDeletionError(throwable);
                        })
        );
    }

}
