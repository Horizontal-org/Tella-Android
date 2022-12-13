package rs.readahead.washington.mobile.util.jobs;

import androidx.annotation.NonNull;

import com.evernote.android.job.Job;
import com.evernote.android.job.JobManager;
import com.evernote.android.job.JobRequest;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.hzontal.tella_vault.VaultFile;

import org.hzontal.tella.keys.key.LifecycleMainKey;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;

import javax.inject.Inject;

import io.reactivex.Flowable;
import io.reactivex.schedulers.Schedulers;
import rs.readahead.washington.mobile.MyApplication;
import rs.readahead.washington.mobile.bus.event.FileUploadProgressEvent;
import rs.readahead.washington.mobile.data.database.DataSource;
import rs.readahead.washington.mobile.data.entity.reports.ReportBodyEntity;
import rs.readahead.washington.mobile.data.sharedpref.Preferences;
import rs.readahead.washington.mobile.domain.entity.EntityStatus;
import rs.readahead.washington.mobile.domain.entity.UploadProgressInfo;
import rs.readahead.washington.mobile.domain.entity.reports.ReportFormInstance;
import rs.readahead.washington.mobile.domain.entity.reports.TellaReportServer;
import rs.readahead.washington.mobile.domain.exception.NoConnectivityException;
import rs.readahead.washington.mobile.domain.repository.ITellaUploadsRepository;
import rs.readahead.washington.mobile.domain.repository.reports.ReportsRepository;
import rs.readahead.washington.mobile.util.ThreadUtil;
import timber.log.Timber;

public class TellaRequestUploadJob extends Job {
    static final String TAG = "TellaRequestUploadJob";
    private static boolean running = false;
    private final HashMap<String, VaultFile> fileMap = new HashMap<>();
    @Inject
    ReportsRepository reportsRepository;
    private Job.Result exitResult = null;
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
        List<ReportFormInstance> reportFormInstances = dataSource.getFileUploadReportBundles(ITellaUploadsRepository.UploadStatus.SCHEDULED).blockingGet();

        if (reportFormInstances.size() == 0) {
            return exit(Result.SUCCESS);
        }

        //3 STEPS
        //First submit report with title and description
        //First upload has highest priority, we are going to upload just to that server
        //SECOND get report if response was successful
        //Third submit files with reportID


        //Grab the server instance from the server
        for (ReportFormInstance reportFormInstance : reportFormInstances) {
            long serverId = reportFormInstance.getServerId();
            server = dataSource.getTellaUploadServer(serverId).blockingGet();

            if (reportFormInstance.getStatus() == EntityStatus.SUBMISSION_PENDING) {
                if (reportFormInstance.getReportApiId().isEmpty()) {
                    reportsRepository.submitReport(server, new ReportBodyEntity(reportFormInstance.getTitle(), reportFormInstance.getDescription()))
                            .subscribeOn(Schedulers.io())
                            .doOnError(Timber::e)
                            .subscribe(reportPostResult -> Flowable.fromIterable(reportFormInstance.getWidgetMediaFiles())
                                    .flatMap(file -> reportsRepository.upload(file, server.getUrl(), reportPostResult.getId(), server.getAccessToken()))
                                    .blockingSubscribe(this::updateProgress, throwable -> {
                                        if (throwable instanceof NoConnectivityException) {
                                            exitResult = Result.RESCHEDULE;
                                            return;
                                        }
                                        Timber.d(throwable);
                                        FirebaseCrashlytics.getInstance().recordException(throwable);
                                    })).dispose();
                } else {
                    Flowable.fromIterable(reportFormInstance.getWidgetMediaFiles())
                            .flatMap(file -> reportsRepository.upload(file, server.getUrl(), reportFormInstance.getReportApiId(), server.getAccessToken()))
                            .blockingSubscribe(this::updateProgress, throwable -> {
                                if (throwable instanceof NoConnectivityException) {
                                    exitResult = Result.RESCHEDULE;
                                    return;
                                }
                                Timber.d(throwable);
                                FirebaseCrashlytics.getInstance().recordException(throwable);
                            });
                }

            }

            if (server != TellaReportServer.NONE) {
                return exit(Result.FAILURE);
            }

        }

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
                dataSource.setUploadStatus(progressInfo.fileId, ITellaUploadsRepository.UploadStatus.UPLOADING, progressInfo.current, false).blockingAwait();
                break;

            case CONFLICT:
            case FINISHED:
                dataSource.setUploadStatus(progressInfo.fileId, ITellaUploadsRepository.UploadStatus.UPLOADED, progressInfo.current, false).blockingAwait();
                if (Preferences.isAutoDeleteEnabled()) {
                    deleteMediaFile(progressInfo.fileId);
                }
                break;

            case ERROR:
                dataSource.setUploadStatus(progressInfo.fileId, ITellaUploadsRepository.UploadStatus.SCHEDULED, progressInfo.current, true).blockingAwait();
                break;

            default:
                dataSource.setUploadStatus(progressInfo.fileId, ITellaUploadsRepository.UploadStatus.UNKNOWN, progressInfo.current, true).blockingAwait();
                break;
        }
        postProgressEvent(progressInfo);
    }

    private void updateReportInstanceStatus(UploadProgressInfo progressInfo) {

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
