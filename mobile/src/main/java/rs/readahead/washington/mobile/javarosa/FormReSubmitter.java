package rs.readahead.washington.mobile.javarosa;

import android.content.Context;

import com.google.firebase.crashlytics.FirebaseCrashlytics;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.Single;
import io.reactivex.SingleSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import rs.readahead.washington.mobile.MyApplication;
import rs.readahead.washington.mobile.data.database.DataSource;
import rs.readahead.washington.mobile.data.database.KeyDataSource;
import rs.readahead.washington.mobile.data.repository.OpenRosaRepository;
import rs.readahead.washington.mobile.data.sharedpref.Preferences;
import rs.readahead.washington.mobile.domain.entity.IProgressListener;
import rs.readahead.washington.mobile.domain.entity.collect.CollectFormInstance;
import rs.readahead.washington.mobile.domain.entity.collect.CollectFormInstanceStatus;
import rs.readahead.washington.mobile.domain.entity.collect.CollectServer;
import rs.readahead.washington.mobile.domain.entity.collect.FormMediaFile;
import rs.readahead.washington.mobile.domain.entity.collect.FormMediaFileStatus;
import rs.readahead.washington.mobile.domain.entity.collect.NegotiatedCollectServer;
import rs.readahead.washington.mobile.domain.entity.collect.OpenRosaPartResponse;
import rs.readahead.washington.mobile.domain.exception.NoConnectivityException;
import rs.readahead.washington.mobile.domain.repository.IOpenRosaRepository;
import rs.readahead.washington.mobile.util.C;
import rs.readahead.washington.mobile.util.Util;


public class FormReSubmitter implements IFormReSubmitterContract.IFormReSubmitter {
    private IFormReSubmitterContract.IView view;
    private KeyDataSource keyDataSource;
    private CompositeDisposable disposables = new CompositeDisposable();
    private IOpenRosaRepository openRosaRepository;
    private Context context;


    public FormReSubmitter(IFormReSubmitterContract.IView view) {
        this.view = view;
        this.context = view.getContext().getApplicationContext();
        this.openRosaRepository = new OpenRosaRepository();
        this.keyDataSource = MyApplication.getKeyDataSource();
    }

    @Override
    public void reSubmitFormInstanceGranular(final CollectFormInstance instance) {
        final boolean offlineMode = Preferences.isOfflineMode();
        final CollectFormInstanceStatus startStatus = instance.getStatus();

        disposables.add(keyDataSource.getDataSource()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe(disposable ->
                        view.showReFormSubmitLoading(instance))
                .flatMapSingle((Function<DataSource, SingleSource<CollectServer>>) dataSource
                        -> setFormDef(dataSource, instance))
                .flatMapSingle((Function<CollectServer, SingleSource<NegotiatedCollectServer>>) server
                        -> negotiateServer(server, offlineMode))
                .flatMap((Function<NegotiatedCollectServer, ObservableSource<List<GranularResubmissionBundle>>>) negotiatedCollectServer ->
                        createPartBundles(instance, negotiatedCollectServer))
                .flatMap(Observable::fromIterable)
                .concatMap((Function<GranularResubmissionBundle, ObservableSource<OpenRosaPartResponse>>) bundle -> {
                    view.formPartResubmitStart(instance, bundle.getPartName());
                    return openRosaRepository.submitFormGranular(context, bundle.server, instance, bundle.attachment,
                            new ProgressListener(bundle.getPartName(), this.view)).toObservable();
                })
                .flatMap((Function<OpenRosaPartResponse, ObservableSource<OpenRosaPartResponse>>) response -> {
                    // set form and attachments statuses
                    setPartSuccessSubmissionStatuses(instance, response.getPartName());
                    return rxSaveSuccessInstance(instance, response);
                })
                .onErrorResumeNext(throwable -> {
                    setErrorSubmissionStatuses(instance, startStatus, throwable);
                    return rxSaveErrorInstance(instance, throwable);
                })
                .doFinally(() -> view.hideReFormSubmitLoading())
                .subscribe(
                        response -> view.formPartResubmitSuccess(instance, response),
                        throwable -> {
                            if (throwable instanceof OfflineModeException) {
                                view.formResubmitOfflineMode();
                            } else if (throwable instanceof NoConnectivityException) {
                                // PendingFormSendJob.scheduleJob();
                                view.formReSubmitNoConnectivity();
                            } else {
                                FirebaseCrashlytics.getInstance().recordException(throwable);
                                view.formPartReSubmitError(throwable);
                            }
                        },
                        () -> view.formPartsResubmitEnded(instance)
                )
        );
    }

    @Override
    public boolean isReSubmitting() {
        return disposables.size() > 0;
    }

    @Override
    public void userStopReSubmission() {
        stopReSubmission();
        view.submissionStoppedByUser();
    }

    @Override
    public void stopReSubmission() {
        disposables.clear();
    }

    @Override
    public void destroy() {
        disposables.dispose();
        view = null;
    }

    private Single<CollectServer> setFormDef(DataSource dataSource, CollectFormInstance instance) {
        return dataSource.getInstance(instance.getId())
                .flatMap((Function<CollectFormInstance, SingleSource<CollectServer>>) fullInstance -> {
                    instance.setFormDef(fullInstance.getFormDef()); // todo: think about this..
                    return dataSource.getCollectServer(instance.getServerId());
                });
    }

    private Single<NegotiatedCollectServer> negotiateServer(CollectServer server, boolean offlineMode)
            throws OfflineModeException, NoConnectivityException {
        if (offlineMode) {
            throw new OfflineModeException();
        }

        if (!MyApplication.isConnectedToInternet(view.getContext())) {
            throw new NoConnectivityException();
        }

        return openRosaRepository.submitFormNegotiate(server);
    }

    @SuppressWarnings("MethodOnlyUsedFromInnerClass")
    private <T> ObservableSource<T> rxSaveSuccessInstance(final CollectFormInstance instance, final T value) {
        return keyDataSource.getDataSource().flatMap((Function<DataSource, ObservableSource<T>>) dataSource ->
                dataSource.saveInstance(instance)
                        .toObservable()
                        .flatMap((Function<CollectFormInstance, ObservableSource<T>>) instance1 -> Observable.just(value)));
    }

    @SuppressWarnings("MethodOnlyUsedFromInnerClass")
    private <T> ObservableSource<T> rxSaveErrorInstance(final CollectFormInstance instance, final Throwable throwable) {
        return keyDataSource.getDataSource().flatMap((Function<DataSource, ObservableSource<T>>) dataSource ->
                dataSource.saveInstance(instance)
                        .toObservable()
                        .flatMap((Function<CollectFormInstance, ObservableSource<T>>) instance1 -> Observable.error(throwable)));
    }

    private void setErrorSubmissionStatuses(CollectFormInstance instance, CollectFormInstanceStatus startStatus, Throwable throwable) {
        CollectFormInstanceStatus status;

        if (startStatus == CollectFormInstanceStatus.SUBMISSION_PARTIAL_PARTS) {
            status = startStatus;
        } else if (throwable instanceof OfflineModeException || throwable instanceof NoConnectivityException) {
            status = CollectFormInstanceStatus.SUBMISSION_PENDING;
        } else {
            status = CollectFormInstanceStatus.SUBMISSION_ERROR;
        }

        instance.setStatus(status);
    }

    private void setPartSuccessSubmissionStatuses(CollectFormInstance instance, String partName) {
        CollectFormInstanceStatus status = CollectFormInstanceStatus.SUBMITTED;

        if (C.OPEN_ROSA_XML_PART_NAME.equals(partName)) { // from xml data part is submitted
            instance.setFormPartStatus(FormMediaFileStatus.SUBMITTED);
            if (!instance.getWidgetMediaFiles().isEmpty()) {
                status = CollectFormInstanceStatus.SUBMISSION_PARTIAL_PARTS;
            }
        } else {
            // update part status
            for (FormMediaFile mediaFile : instance.getWidgetMediaFiles()) {
                if (mediaFile.getPartName().equals(partName)) {
                    mediaFile.status = FormMediaFileStatus.SUBMITTED;
                    break;
                }
            }

            // check instance status
            for (FormMediaFile mediaFile : instance.getWidgetMediaFiles()) {
                if (mediaFile.status != FormMediaFileStatus.SUBMITTED) {
                    status = CollectFormInstanceStatus.SUBMISSION_PARTIAL_PARTS;
                    break;
                }
            }
        }

        instance.setStatus(status);
    }

    private Observable<List<GranularResubmissionBundle>> createPartBundles(CollectFormInstance instance, NegotiatedCollectServer server) {
        List<GranularResubmissionBundle> bundles = new ArrayList<>();

        if (instance.getStatus() != CollectFormInstanceStatus.SUBMISSION_PARTIAL_PARTS &&
                instance.getStatus() != CollectFormInstanceStatus.SUBMITTED) {
            bundles.add(new GranularResubmissionBundle(server));
        }

        for (FormMediaFile attachment : instance.getWidgetMediaFiles()) {
            if (attachment.uploading && attachment.status != FormMediaFileStatus.SUBMITTED) {
                bundles.add(new GranularResubmissionBundle(server, attachment));
            }
        }

        return Observable.just(bundles);
    }

    private static class OfflineModeException extends Exception {
    }

    private static class GranularResubmissionBundle {
        NegotiatedCollectServer server;
        FormMediaFile attachment;

        GranularResubmissionBundle(NegotiatedCollectServer server) {
            this.server = server;
        }

        GranularResubmissionBundle(NegotiatedCollectServer server, FormMediaFile attachment) {
            this.server = server;
            this.attachment = attachment;
        }

        String getPartName() {
            return attachment != null ? attachment.getPartName() : C.OPEN_ROSA_XML_PART_NAME;
        }
    }

    // todo: merge IForm{Re}Submitter presenter interfaces
    static class ProgressListener implements IProgressListener {
        private static final long REFRESH_TIME_MS = 500;
        private String partName;
        private IFormReSubmitterContract.IView view;
        private long time;

        ProgressListener(String partName, IFormReSubmitterContract.IView view) {
            this.partName = partName;
            this.view = view;
        }

        @Override
        public void onProgressUpdate(long current, long total) {
            long now = Util.currentTimestamp();

            if (view != null && now - time > REFRESH_TIME_MS) {
                time = now;
                view.formPartUploadProgress(partName, (float) current / (float) total);
            }
        }
    }
}
