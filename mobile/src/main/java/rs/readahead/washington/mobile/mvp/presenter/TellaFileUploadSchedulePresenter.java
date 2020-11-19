package rs.readahead.washington.mobile.mvp.presenter;

import com.google.firebase.crashlytics.FirebaseCrashlytics;

import java.util.List;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import rs.readahead.washington.mobile.data.database.CacheWordDataSource;
import rs.readahead.washington.mobile.domain.entity.MediaFile;
import rs.readahead.washington.mobile.mvp.contract.ITellaFileUploadSchedulePresenterContract;
import rs.readahead.washington.mobile.util.jobs.TellaUploadJob;


public class TellaFileUploadSchedulePresenter implements ITellaFileUploadSchedulePresenterContract.IPresenter {
    private ITellaFileUploadSchedulePresenterContract.IView view;
    private CompositeDisposable disposables = new CompositeDisposable();
    private CacheWordDataSource cacheWordDataSource;


    public TellaFileUploadSchedulePresenter(ITellaFileUploadSchedulePresenterContract.IView view) {
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
    public void scheduleUploadMediaFiles(final List<MediaFile> mediaFiles) {
        disposables.add(cacheWordDataSource.getDataSource()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .flatMapCompletable(dataSource -> dataSource.scheduleUploadMediaFiles(mediaFiles))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(() -> {
                    TellaUploadJob.scheduleJob();
                    view.onMediaFilesUploadScheduled();
                }, throwable -> {
                    FirebaseCrashlytics.getInstance().recordException(throwable);
                    view.onMediaFilesUploadScheduleError(throwable);
                })
        );
    }

    @Override
    public void scheduleUploadMediaFilesWithPriority(final List<MediaFile> mediaFiles, long uploadServerId, boolean metadata) {
        disposables.add(cacheWordDataSource.getDataSource()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .flatMapCompletable(dataSource -> dataSource.scheduleUploadMediaFilesWithPriority(mediaFiles, uploadServerId, metadata))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(() -> {
                    TellaUploadJob.cancelJob();
                    TellaUploadJob.scheduleJob();
                    view.onMediaFilesUploadScheduledWithPriority();
                }, throwable -> {
                    FirebaseCrashlytics.getInstance().recordException(throwable);
                    view.onMediaFilesUploadScheduleError(throwable);
                })
        );
    }
}
