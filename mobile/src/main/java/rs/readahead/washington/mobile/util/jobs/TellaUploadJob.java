package rs.readahead.washington.mobile.util.jobs;

import com.evernote.android.job.Job;
import com.evernote.android.job.JobManager;
import com.evernote.android.job.JobRequest;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.hzontal.tella_vault.VaultFile;

import org.hzontal.tella.keys.key.LifecycleMainKey;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import androidx.annotation.NonNull;
import io.reactivex.Flowable;
import io.reactivex.schedulers.Schedulers;
import rs.readahead.washington.mobile.MyApplication;
import rs.readahead.washington.mobile.bus.event.FileUploadProgressEvent;
import rs.readahead.washington.mobile.data.database.DataSource;
import rs.readahead.washington.mobile.data.sharedpref.Preferences;
import rs.readahead.washington.mobile.data.upload.TUSClient;
import rs.readahead.washington.mobile.domain.entity.FileUploadBundle;
import rs.readahead.washington.mobile.domain.entity.reports.TellaReportServer;
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
    private final HashMap<String, VaultFile> fileMap = new HashMap<>();
    private DataSource dataSource;
    private TellaReportServer server;

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

    public static void cancelJob() {
        JobManager.instance().cancelAllForTag(TellaUploadJob.TAG);
    }

    @NonNull
    @Override
    protected Result onRunJob(@NotNull Job.Params params) {
        if (!enter()) {
            return Result.SUCCESS;
        }

        if (Preferences.isAutoUploadPaused()) {
            return exit(Result.RESCHEDULE);
        }

        byte[] key;
        try {
            if ((key = MyApplication.getMainKeyHolder().get().getKey().getEncoded()) == null) {
                return exit(Result.RESCHEDULE);
            }
        } catch (LifecycleMainKey.MainKeyUnavailableException e) {
            Timber.d(e);
            return exit(Result.RESCHEDULE);
        }


        dataSource = DataSource.getInstance(getContext(), key);
        List<FileUploadBundle> fileUploadBundles = dataSource.getFileUploadBundles(UploadStatus.SCHEDULED).blockingGet();

        if (fileUploadBundles.size() == 0) {
            return exit(Result.SUCCESS);
        }

        //First upload has highest priority, we are going to upload just to that server
        for (FileUploadBundle fileUploadBundle : fileUploadBundles) {
            long serverId = fileUploadBundle.getServerId();
            server = dataSource.getTellaUploadServer(serverId).blockingGet();

            if (server != TellaReportServer.NONE) {
                break;
            }
        }

        if (server == TellaReportServer.NONE) {
            return exit(Result.FAILURE);
        }

        List<VaultFile> vaultFiles = new ArrayList<>();
        for (FileUploadBundle fileUploadBundle : fileUploadBundles) {

            if (fileUploadBundle.getServerId() != server.getId()) {
                continue;
            } else {
                vaultFiles.add(fileUploadBundle.getMediaFile());
            }

            if (!fileUploadBundle.isManualUpload()) {
                fileMap.put(fileUploadBundle.getMediaFile().id, fileUploadBundle.getMediaFile());
            }

            if (fileUploadBundle.isIncludeMetdata()) {
                try {
                    vaultFiles.add(MediaFileHandler.maybeCreateMetadataMediaFile(fileUploadBundle.getMediaFile()));
                } catch (Exception e) {
                    Timber.d(e);
                }
            }
        }

        final TUSClient tusClient = new TUSClient(getContext(), server.getUrl(), server.getUsername(), server.getPassword());

        Flowable.fromIterable(vaultFiles)
                .flatMap(tusClient::upload)
                .blockingSubscribe(this::updateProgress, throwable -> {
                    if (throwable instanceof NoConnectivityException) {
                        exitResult = Result.RESCHEDULE;
                        return;
                    }
                    Timber.d(throwable);
                    FirebaseCrashlytics.getInstance().recordException(throwable);
                });

        if (exitResult != null) {
            return exit(exitResult);
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

    private void updateProgress(UploadProgressInfo progressInfo) {
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

    private void deleteMediaFile(String id) {
        if (MyApplication.rxVault.get(id)
                .flatMap(vaultFile -> MyApplication.rxVault.delete(vaultFile))
                .subscribeOn(Schedulers.io())
                .blockingGet()) {
            Timber.d("Deleted file %s", id);
        }
    }

    private void postProgressEvent(UploadProgressInfo progress) {
        ThreadUtil.runOnMain(() -> MyApplication.bus().post(new FileUploadProgressEvent(progress)));
    }
}