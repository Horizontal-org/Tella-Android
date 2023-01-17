package rs.readahead.washington.mobile.mvp.presenter;

//import com.google.firebase.crashlytics.FirebaseCrashlytics;

import java.util.List;

import io.reactivex.SingleSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import rs.readahead.washington.mobile.MyApplication;
import rs.readahead.washington.mobile.data.database.DataSource;
import rs.readahead.washington.mobile.data.database.KeyDataSource;
import rs.readahead.washington.mobile.domain.entity.FileUploadInstance;
import rs.readahead.washington.mobile.domain.repository.ITellaUploadsRepository;
import rs.readahead.washington.mobile.mvp.contract.ITellaFileUploadPresenterContract;
import timber.log.Timber;

public class TellaFileUploadPresenter implements ITellaFileUploadPresenterContract.IPresenter {
    private ITellaFileUploadPresenterContract.IView view;
    private CompositeDisposable disposables = new CompositeDisposable();
    private KeyDataSource keyDataSource;


    public TellaFileUploadPresenter(ITellaFileUploadPresenterContract.IView view) {
        this.view = view;
        this.keyDataSource = MyApplication.getKeyDataSource();
    }

    @Override
    public void destroy() {
        disposables.dispose();
        view = null;
    }

    @Override
    public void getFileUploadInstances() {
        disposables.add(keyDataSource.getDataSource()
                .flatMapSingle((Function<DataSource, SingleSource<List<FileUploadInstance>>>) DataSource::getFileUploadInstances)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(filesUploadInstances -> view.onGetFileUploadInstancesSuccess(filesUploadInstances), throwable -> {
                    Timber.e(throwable);
                    view.onGetFileUploadInstancesError(throwable);
                })
        );
    }

    @Override
    public void getFileUploadSetInstances(long set) {
        disposables.add(keyDataSource.getDataSource()
                .flatMapSingle((Function<DataSource, SingleSource<List<FileUploadInstance>>>) dataSource -> dataSource.getFileUploadInstances(set))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(filesUploadInstances -> view.onGetFileUploadSetInstancesSuccess(filesUploadInstances), throwable -> {
                    Timber.e(throwable);
                    view.onGetFileUploadSetInstancesError(throwable);
                })
        );
    }

    @Override
    public void deleteFileUploadInstance(long id) {
        disposables.add(keyDataSource.getDataSource()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .flatMapCompletable(dataSource -> dataSource.deleteFileUploadInstanceById(id))
                .subscribe(() -> view.onFileUploadInstancesDeleted(),
                        throwable -> {
                            Timber.e(throwable);
                            view.onFileUploadInstancesDeletionError(throwable);
                        })
        );
    }

    @Override
    public void deleteFileUploadInstances(long set) {
        disposables.add(keyDataSource.getDataSource()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .flatMapCompletable(dataSource -> dataSource.deleteFileUploadInstancesBySet(set))
                .subscribe(() -> view.onFileUploadInstancesDeleted(),
                        throwable -> {
                            Timber.e(throwable);
                            view.onFileUploadInstancesDeletionError(throwable);
                        })
        );
    }

    @Override
    public void deleteFileUploadInstancesInStatus(ITellaUploadsRepository.UploadStatus status) {
        disposables.add(keyDataSource.getDataSource()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .flatMapCompletable(dataSource -> dataSource.deleteFileUploadInstancesInStatus(status))
                .subscribe(() -> view.onFileUploadInstancesDeleted(),
                        throwable -> {
                            Timber.e(throwable);
                            view.onFileUploadInstancesDeletionError(throwable);
                        })
        );
    }

    @Override
    public void deleteFileUploadInstancesNotInStatus(ITellaUploadsRepository.UploadStatus status) {
        disposables.add(keyDataSource.getDataSource()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .flatMapCompletable(dataSource -> dataSource.deleteFileUploadInstancesNotInStatus(status))
                .subscribe(() -> view.onFileUploadInstancesDeleted(),
                        throwable -> {
                            Timber.e(throwable);
                            view.onFileUploadInstancesDeletionError(throwable);
                        })
        );
    }

}
