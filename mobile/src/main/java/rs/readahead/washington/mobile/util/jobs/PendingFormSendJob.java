package rs.readahead.washington.mobile.util.jobs;

import android.support.annotation.NonNull;

import com.evernote.android.job.Job;
import com.evernote.android.job.JobRequest;

import java.util.List;

import io.reactivex.Observable;
import io.reactivex.SingleSource;
import io.reactivex.functions.Function;
import rs.readahead.washington.mobile.MyApplication;
import rs.readahead.washington.mobile.bus.event.CollectFormSubmittedEvent;
import rs.readahead.washington.mobile.data.database.DataSource;
import rs.readahead.washington.mobile.data.repository.OpenRosaRepository;
import rs.readahead.washington.mobile.domain.entity.KeyBundle;
import rs.readahead.washington.mobile.domain.entity.collect.CollectFormInstance;
import rs.readahead.washington.mobile.domain.entity.collect.CollectFormInstanceStatus;
import rs.readahead.washington.mobile.domain.entity.collect.CollectServer;
import rs.readahead.washington.mobile.domain.entity.collect.NegotiatedCollectServer;
import rs.readahead.washington.mobile.domain.entity.collect.OpenRosaResponse;
import rs.readahead.washington.mobile.domain.exception.NoConnectivityException;
import rs.readahead.washington.mobile.domain.repository.IOpenRosaRepository;
import rs.readahead.washington.mobile.util.ThreadUtil;


public class PendingFormSendJob extends Job {
    static final String TAG = "PendingFormSendJob";
    private static boolean running = false;
    private Result exitResult = null;


    @NonNull
    @Override
    protected Result onRunJob(Params params) {
        if (! enter()) {
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

        final IOpenRosaRepository openRosaRepository = new OpenRosaRepository();
        final DataSource dataSource = DataSource.getInstance(getContext(), key);

        List<CollectFormInstance> pendingInstances = dataSource.listPendingForms().blockingGet();

        if (pendingInstances.size() == 0) {
            return exit(Result.SUCCESS);
        }

        for (final CollectFormInstance instance: pendingInstances) {
            if (instance.getStatus() != CollectFormInstanceStatus.SUBMISSION_PENDING) {
                continue;
            }

            Observable.just(dataSource)
                    .flatMapSingle((Function<DataSource, SingleSource<CollectServer>>) dataSource1 ->
                            dataSource1.getCollectServer(instance.getServerId()))
                    .flatMapSingle((Function<CollectServer, SingleSource<NegotiatedCollectServer>>) server -> {
                        if (!MyApplication.isConnectedToInternet(getContext())) {
                            throw new NoConnectivityException();
                        }

                        return openRosaRepository.submitFormNegotiate(server);
                    })
                    .flatMapSingle((Function<NegotiatedCollectServer, SingleSource<OpenRosaResponse>>) server ->
                            openRosaRepository.submitForm(getContext(), server, instance))
                    .blockingSubscribe(
                            response -> {
                                instance.setStatus(CollectFormInstanceStatus.SUBMITTED);
                                //noinspection ResultOfMethodCallIgnored
                                dataSource.saveInstance(instance).blockingGet();
                                postSubmittedEvent();
                            }, throwable -> {
                                if (throwable instanceof NoConnectivityException) {
                                    exitResult = Result.RESCHEDULE;
                                    return;
                                }

                                instance.setStatus(CollectFormInstanceStatus.SUBMISSION_ERROR);
                                //noinspection ResultOfMethodCallIgnored
                                dataSource.saveInstance(instance).blockingGet();
                                postSubmittedEvent();
                            });

            if (exitResult != null) {
                exit(exitResult);
            }
        }

        return exit(Result.SUCCESS);
    }

    @Override
    protected void onReschedule(int newJobId) {
    }

    private boolean enter() {
        synchronized (PendingFormSendJob.class) {
            boolean current = running;
            running = true;
            return !current;
        }
    }

    private Result exit(Result result) {
        synchronized (PendingFormSendJob.class) {
            running = false;
            return result;
        }
    }

    public static void scheduleJob() {
        new JobRequest.Builder(PendingFormSendJob.TAG)
                .setExecutionWindow(1_000L, 10_000L) // start between 1-10sec from now
                .setBackoffCriteria(10_000L, JobRequest.BackoffPolicy.LINEAR)
                .setRequiredNetworkType(JobRequest.NetworkType.CONNECTED)
                .setRequirementsEnforced(true)
                .setPersisted(true) // android.Manifest.permission.RECEIVE_BOOT_COMPLETED
                .setUpdateCurrent(true) // also, jobs will sync them self while running
                .build()
                .schedule();
    }

    @SuppressWarnings("MethodOnlyUsedFromInnerClass")
    private void postSubmittedEvent() {
        ThreadUtil.runOnMain(() -> MyApplication.bus().post(new CollectFormSubmittedEvent()));
    }
}
