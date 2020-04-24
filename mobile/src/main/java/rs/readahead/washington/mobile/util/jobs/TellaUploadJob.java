package rs.readahead.washington.mobile.util.jobs;

import androidx.annotation.NonNull;

import com.crashlytics.android.Crashlytics;
import com.evernote.android.job.Job;
import com.evernote.android.job.JobRequest;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;

import io.reactivex.Flowable;
import rs.readahead.washington.mobile.MyApplication;
import rs.readahead.washington.mobile.data.database.DataSource;
import rs.readahead.washington.mobile.data.sharedpref.Preferences;
import rs.readahead.washington.mobile.data.upload.TUSClient;
import rs.readahead.washington.mobile.domain.entity.KeyBundle;
import rs.readahead.washington.mobile.domain.entity.MediaFile;
import rs.readahead.washington.mobile.domain.entity.TellaUploadServer;
import rs.readahead.washington.mobile.domain.entity.UploadProgressInfo;
import rs.readahead.washington.mobile.domain.exception.NoConnectivityException;
import rs.readahead.washington.mobile.domain.repository.ITellaUploadsRepository;
import rs.readahead.washington.mobile.media.MediaFileHandler;
import timber.log.Timber;

public class TellaUploadJob extends Job {
    static final String TAG = "TellaUploadJob";
    private static boolean running = false;
    private Job.Result exitResult = null;
    private HashMap<String, MediaFile> fileMap = new HashMap<>();
    private DataSource dataSource;

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

        dataSource = DataSource.getInstance(getContext(), key);
        List<MediaFile> mediaFiles = dataSource.getUploadMediaFiles(ITellaUploadsRepository.UploadStatus.SCHEDULED).blockingGet();

        if (mediaFiles.size() == 0) {
            return exit(Job.Result.SUCCESS);
        }

        TellaUploadServer server = dataSource.getTellaUploadServer(Preferences.getAutoUploadServerId()).blockingGet();
        if (server == TellaUploadServer.NONE){
            return exit(Result.FAILURE);
        }
        final TUSClient tusClient = new TUSClient(getContext(), server.getUrl(), server.getUsername(), server.getPassword());

        for (MediaFile file : mediaFiles) {
            fileMap.put(String.valueOf(file.getFileName()), file);
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

    private void updateProgress(UploadProgressInfo progressInfo) {

        if (fileMap.get(progressInfo.name) == null) return;

        MediaFile mediaFile = fileMap.get(progressInfo.name);
        assert mediaFile != null;

        switch (progressInfo.status) {

            case STARTED:
                dataSource.setUploadStatus(mediaFile.getId(), ITellaUploadsRepository.UploadStatus.UPLOADING).blockingAwait();
                break;

            case OK:
                dataSource.setUploadedAmount(mediaFile.getId(), progressInfo.current).blockingAwait();
                break;

            case CONFLICT:
            case FINISHED:
                dataSource.setUploadStatus(mediaFile.getId(), ITellaUploadsRepository.UploadStatus.UPLOADED).blockingAwait();
                if (Preferences.isAutoDeleteEnabled()) {
                    autoDeleteMediaFile(mediaFile);
                }
                break;

            case ERROR:
                dataSource.setUploadReschedule(mediaFile.getId()).blockingAwait();
                break;

            default:
                dataSource.setUploadStatus(mediaFile.getId(), ITellaUploadsRepository.UploadStatus.ERROR).blockingAwait();
                break;
        }
    }

    private void autoDeleteMediaFile(MediaFile mediaFile) {
        MediaFile deleted = dataSource.deleteMediaFile(mediaFile, mediaFile1 ->
                MediaFileHandler.deleteMediaFile(getContext(), mediaFile1)).blockingGet();
        Timber.d("Deleted file %s", deleted.getFileName());
    }
}
