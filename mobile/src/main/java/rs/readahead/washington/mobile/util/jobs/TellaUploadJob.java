package rs.readahead.washington.mobile.util.jobs;

import androidx.annotation.NonNull;

import com.crashlytics.android.Crashlytics;
import com.evernote.android.job.Job;
import com.evernote.android.job.JobRequest;

import org.jetbrains.annotations.NotNull;

import java.util.List;

import io.reactivex.Flowable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import rs.readahead.washington.mobile.MyApplication;
import rs.readahead.washington.mobile.data.database.DataSource;
import rs.readahead.washington.mobile.data.sharedpref.Preferences;
import rs.readahead.washington.mobile.data.upload.TUSClient;
import rs.readahead.washington.mobile.domain.entity.KeyBundle;
import rs.readahead.washington.mobile.domain.entity.MediaFile;
import rs.readahead.washington.mobile.domain.entity.TellaUploadServer;
import rs.readahead.washington.mobile.domain.entity.UploadProgressInfo;
import rs.readahead.washington.mobile.domain.repository.ITellaUploadsRepository;
import timber.log.Timber;

public class TellaUploadJob extends Job {
    static final String TAG = "TellaUploadJob";
    private static boolean running = false;
    private Job.Result exitResult = null;
    private CompositeDisposable disposables = new CompositeDisposable();

    @NonNull
    @Override
    protected Job.Result onRunJob(@NotNull Job.Params params) {
        if (!enter()) {
            return Job.Result.SUCCESS;
        }

        KeyBundle keyBundle = MyApplication.getKeyBundle();
        if (keyBundle == null) { // CacheWord is unavailable
            return exit(Job.Result.RESCHEDULE);
        }

        byte[] key = keyBundle.getKey();
        if (key == null) { // key disappeared
            return exit(Job.Result.RESCHEDULE);
        }

        DataSource dataSource = DataSource.getInstance(getContext(), key);
        TellaUploadServer server = dataSource.getTellaUploadServer(Preferences.getAutoUploadServerId()).blockingGet();

        List<MediaFile> mediaFiles = dataSource.getUploadMediaFiles(ITellaUploadsRepository.UploadStatus.SCHEDULED).blockingGet();
        final TUSClient tusClient = new TUSClient(getContext(), server.getUrl(), server.getUsername(), server.getPassword());

        if (mediaFiles.size() == 0) {
            return exit(Job.Result.SUCCESS);
        }

        dataSource.setUploadingStatus(mediaFiles, ITellaUploadsRepository.UploadStatus.UPLOADING).blockingAwait();

        disposables.add(Flowable.fromIterable(mediaFiles)
                .flatMap(tusClient::upload)
                .subscribeOn(Schedulers.single())
                .doOnSubscribe(progressInfo -> onJobSubscribe())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnComplete(this::onJobCompleted)
                .doFinally(this::onJobFinalized)
                .subscribe(this::updateProgress, throwable -> {
                    Timber.d(throwable);
                    Crashlytics.logException(throwable);
                })
        );

        dataSource.setUploadingStatus(mediaFiles, ITellaUploadsRepository.UploadStatus.UPLOADED).blockingAwait();

        if (exitResult != null) {
            exit(exitResult);
        }

        return exit(Job.Result.SUCCESS);
    }

    @Override
    protected void onReschedule(int newJobId) {
    }

    private boolean enter() {
        synchronized (TellaUploadJob.class) {
            boolean current = running;
            running = true;
            return !current;
        }
    }

    private Job.Result exit(Job.Result result) {
        synchronized (TellaUploadJob.class) {
            running = false;
            return result;
        }
    }

    public static void scheduleJob() {
        new JobRequest.Builder(TellaUploadJob.TAG)
                .setExecutionWindow(1_000L, 10_000L) // start between 1-10sec from now
                .setBackoffCriteria(10_000L, JobRequest.BackoffPolicy.LINEAR)
                .setRequiredNetworkType(JobRequest.NetworkType.CONNECTED)
                .setRequirementsEnforced(true)
                //.setPersisted(true) // android.Manifest.permission.RECEIVE_BOOT_COMPLETED
                .setUpdateCurrent(true) // also, jobs will sync them self while running
                .build()
                .schedule();
    }

    private void onJobCompleted() {
    }

    private void updateProgress(UploadProgressInfo progressInfo) {
    }

    private void onJobFinalized() {
    }

    private void onJobSubscribe() {
    }
}
