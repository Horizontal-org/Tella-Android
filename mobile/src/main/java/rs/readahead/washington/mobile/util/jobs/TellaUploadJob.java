package rs.readahead.washington.mobile.util.jobs;

import com.crashlytics.android.Crashlytics;
import com.evernote.android.job.Job;
import com.evernote.android.job.JobManager;
import com.evernote.android.job.JobRequest;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;

import androidx.annotation.NonNull;
import io.reactivex.Flowable;
import io.reactivex.disposables.Disposable;
import rs.readahead.washington.mobile.MyApplication;
import rs.readahead.washington.mobile.bus.event.FileUploadProgressEvent;
import rs.readahead.washington.mobile.data.database.DataSource;
import rs.readahead.washington.mobile.data.sharedpref.Preferences;
import rs.readahead.washington.mobile.data.upload.TUSClient;
import rs.readahead.washington.mobile.domain.entity.KeyBundle;
import rs.readahead.washington.mobile.domain.entity.MediaFile;
import rs.readahead.washington.mobile.domain.entity.TellaUploadServer;
import rs.readahead.washington.mobile.domain.entity.UploadProgressInfo;
import rs.readahead.washington.mobile.domain.exception.NoConnectivityException;
import rs.readahead.washington.mobile.media.MediaFileHandler;
import rs.readahead.washington.mobile.util.ThreadUtil;
import timber.log.Timber;

import static rs.readahead.washington.mobile.domain.repository.ITellaUploadsRepository.UploadStatus;

public class TellaUploadJob extends Job {
    static final String TAG = "TellaUploadJob";
    private static boolean running = false;
    private Job.Result exitResult = null;
    private HashMap<Long, MediaFile> fileMap = new HashMap<>();
    private DataSource dataSource;

    @NonNull
    @Override
    protected Result onRunJob(@NotNull Job.Params params) {
        if (!enter() || Preferences.isAutoUploadPaused()) {
            return Result.SUCCESS;
        }

        KeyBundle keyBundle = MyApplication.getKeyBundle();
        if (keyBundle == null) { // CacheWord is unavailable
            return exit(Result.RESCHEDULE);
        }

        byte[] key = keyBundle.getKey();
        if (key == null) { // key disappeared
            return exit(Result.RESCHEDULE);
        }

        dataSource = DataSource.getInstance(getContext(), key);
        List<MediaFile> mediaFiles = dataSource.getUploadMediaFiles(UploadStatus.SCHEDULED).blockingGet();

        if (mediaFiles.size() == 0) {
            return exit(Result.SUCCESS);
        }

        for (MediaFile file : mediaFiles){
            Timber.d("++++ file id, uid %s, %s, %s", file.getId(), file.getUid(), file.getType());
        }

        TellaUploadServer server = dataSource.getTellaUploadServer(Preferences.getAutoUploadServerId()).blockingGet();
        if (server == TellaUploadServer.NONE) {
            return exit(Result.FAILURE);
        }

        final TUSClient tusClient = new TUSClient(getContext(), server.getUrl(), server.getUsername(), server.getPassword());

        for (MediaFile file : mediaFiles) {
            fileMap.put(file.getId(), file);
        }

        Flowable.fromIterable(mediaFiles)
                .flatMap(tusClient::upload)
                .blockingSubscribe(this::updateProgress, throwable -> {
                    if (throwable instanceof NoConnectivityException) {
                        exitResult = Result.RESCHEDULE;
                        return;
                    }
                    Timber.d(throwable);
                    Crashlytics.logException(throwable);
                });

        if (exitResult != null) {
            exit(exitResult);
        }

        return exit(Result.SUCCESS);
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

    public static void cancelJob(){
        JobManager.instance().cancelAllForTag(TellaUploadJob.TAG);
    }

    private void updateProgress(UploadProgressInfo progressInfo) {
        Timber.d("++++ fileid, status %s, %s", progressInfo.fileId, progressInfo.status );
        switch (progressInfo.status) {
            case STARTED:
            case OK:
                dataSource.setUploadStatus(progressInfo.fileId, UploadStatus.UPLOADING, progressInfo.current, false).blockingAwait();
                break;

            case CONFLICT:
            case FINISHED:
                dataSource.setUploadStatus(progressInfo.fileId, UploadStatus.UPLOADED, progressInfo.current, false).blockingAwait();
                if (Preferences.isAutoDeleteEnabled()) {
                    deleteMediaFile(progressInfo.fileId);
                }
                break;

            case ERROR:
                dataSource.setUploadStatus(progressInfo.fileId, UploadStatus.SCHEDULED, progressInfo.current, true).blockingAwait();
                break;

            default:
                dataSource.setUploadStatus(progressInfo.fileId, UploadStatus.UNKNOWN, progressInfo.current, true).blockingAwait();
                break;
        }
        postProgressEvent(progressInfo);
    }

    private void deleteMediaFile(long id) {
        MediaFile deleted = dataSource.deleteMediaFile(fileMap.get(id), m ->
                MediaFileHandler.deleteMediaFile(getContext(), m)).blockingGet();

        Timber.d("Deleted file %s", deleted.getFileName());
    }

    private void postProgressEvent(UploadProgressInfo progress) {
        ThreadUtil.runOnMain(() -> MyApplication.bus().post(new FileUploadProgressEvent(progress)));
    }
}
