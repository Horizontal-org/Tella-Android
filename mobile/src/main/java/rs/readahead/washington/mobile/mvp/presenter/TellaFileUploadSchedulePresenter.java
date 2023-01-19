package rs.readahead.washington.mobile.mvp.presenter;

import com.hzontal.tella_vault.VaultFile;

import java.util.List;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import rs.readahead.washington.mobile.MyApplication;
import rs.readahead.washington.mobile.data.database.KeyDataSource;
import rs.readahead.washington.mobile.mvp.contract.ITellaFileUploadSchedulePresenterContract;
import rs.readahead.washington.mobile.util.jobs.TellaUploadJob;
import timber.log.Timber;


public class TellaFileUploadSchedulePresenter implements ITellaFileUploadSchedulePresenterContract.IPresenter {
    private ITellaFileUploadSchedulePresenterContract.IView view;
    private final CompositeDisposable disposables = new CompositeDisposable();
    private final KeyDataSource keyDataSource;


    public TellaFileUploadSchedulePresenter(ITellaFileUploadSchedulePresenterContract.IView view) {
        this.view = view;
        this.keyDataSource = MyApplication.getKeyDataSource();
    }

    @Override
    public void destroy() {
        disposables.dispose();
        view = null;
    }

    @Override
    public void scheduleUploadMediaFiles(final List<VaultFile> mediaFiles) {
        disposables.add(keyDataSource.getDataSource()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .flatMapCompletable(dataSource -> dataSource.scheduleUploadMediaFiles(mediaFiles))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(() -> {
                    TellaUploadJob.scheduleJob();
                    view.onMediaFilesUploadScheduled();
                }, throwable -> {
                    Timber.e(throwable);
                    view.onMediaFilesUploadScheduleError(throwable);
                })
        );
    }

    @Override
    public void scheduleUploadMediaFilesWithPriority(final List<VaultFile> mediaFiles, long uploadServerId, boolean metadata) {
        disposables.add(keyDataSource.getDataSource()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .flatMapCompletable(dataSource -> dataSource.scheduleUploadMediaFilesWithPriority(mediaFiles, uploadServerId, metadata))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(() -> {
                    TellaUploadJob.cancelJob();
                    TellaUploadJob.scheduleJob();
                    view.onMediaFilesUploadScheduled();
                }, throwable -> {
                    Timber.e(throwable);
                    view.onMediaFilesUploadScheduleError(throwable);
                })
        );
    }
}
