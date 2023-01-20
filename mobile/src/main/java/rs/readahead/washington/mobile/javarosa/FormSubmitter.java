package rs.readahead.washington.mobile.javarosa;

import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.Nullable;

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
import rs.readahead.washington.mobile.odk.FormController;
import rs.readahead.washington.mobile.util.C;
import rs.readahead.washington.mobile.util.ThreadUtil;
import rs.readahead.washington.mobile.util.Util;
import timber.log.Timber;


public class FormSubmitter implements IFormSubmitterContract.IFormSubmitter {
    private IFormSubmitterContract.IView view;
    private final CompositeDisposable disposables = new CompositeDisposable();
    private final IOpenRosaRepository openRosaRepository;
    private final Context context;
    private final KeyDataSource keyDataSource;


    public FormSubmitter(IFormSubmitterContract.IView view) {
        this.view = view;
        this.context = view.getContext().getApplicationContext();
        this.openRosaRepository = new OpenRosaRepository();
        this.keyDataSource = MyApplication.getKeyDataSource();
    }

    @Override
    public void submitActiveFormInstance(String name) {
        CollectFormInstance instance = FormController.getActive().getCollectFormInstance();

        if (!TextUtils.isEmpty(name)) {
            instance.setInstanceName(name);
        }

        // submitFormInstance(instance);
        submitFormInstanceGranular(instance);
    }
/*
    @Override
    public void submitFormInstance(final CollectFormInstance instance) {
        final boolean offlineMode = Preferences.isOfflineMode();
        final CollectFormInstanceStatus prevStatus = instance.getStatus();

        disposables.add(keyDataSource.getDataSource()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe(disposable -> view.showFormSubmitLoading(instance))
                .flatMapSingle((Function<DataSource, SingleSource<CollectServer>>) dataSource ->
                        finalizeFormInstance(dataSource, instance))
                .flatMapSingle((Function<CollectServer, SingleSource<NegotiatedCollectServer>>) server ->
                        negotiateServer(server, offlineMode))
                .flatMapSingle((Function<NegotiatedCollectServer, SingleSource<OpenRosaResponse>>) server ->
                        openRosaRepository.submitForm(context, server, instance))
                .flatMap((Function<OpenRosaResponse, ObservableSource<OpenRosaResponse>>) response -> {
                    // set form and attachments statuses
                    setSuccessSubmissionStatuses(instance);
                    return rxSaveFormInstance(instance, response, null);
                })
                .onErrorResumeNext(throwable -> {
                    setErrorSubmissionStatuses(instance, prevStatus, throwable);
                    return rxSaveFormInstance(instance, null, throwable);
                })
                .doFinally(() -> view.hideFormSubmitLoading())
                .subscribe(openRosaResponse -> {
                    // start attachment upload process
                    // if (hasAttachments(instance)) {
                     //   updateMediaFilesQueue(instance.getMediaFiles());
                   // }

                    view.formSubmitSuccess(instance, openRosaResponse);
                }, throwable -> {
                    if (throwable instanceof OfflineModeException) {
                        view.formSubmitOfflineMode();
                    } else if (throwable instanceof NoConnectivityException) {
                        // PendingFormSendJob.scheduleJob();
                        view.formSubmitNoConnectivity();
                    } else {
                        FirebaseCrashlytics.getInstance().recordException(throwable);
                        view.formSubmitError(throwable);
                    }
                })
        );
    }*/

    @Override
    public void submitFormInstanceGranular(final CollectFormInstance instance) {
        final boolean offlineMode = false;
        final CollectFormInstanceStatus startStatus = instance.getStatus();

        disposables.add(keyDataSource.getDataSource()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe(disposable ->
                        view.showFormSubmitLoading(instance))
                .flatMapSingle((Function<DataSource, SingleSource<CollectServer>>) dataSource
                        -> finalizeFormInstance(dataSource, instance))
                .flatMapSingle((Function<CollectServer, SingleSource<NegotiatedCollectServer>>) server
                        -> negotiateServer(server, offlineMode))
                .flatMap((Function<NegotiatedCollectServer, ObservableSource<List<GranularSubmissionBundle>>>) negotiatedCollectServer ->
                        createPartBundles(instance, negotiatedCollectServer))
                .flatMap(Observable::fromIterable)
                .concatMap((Function<GranularSubmissionBundle, ObservableSource<OpenRosaPartResponse>>) bundle -> {
                    view.formPartSubmitStart(instance, bundle.getPartName());
                    return openRosaRepository.submitFormGranular(context, bundle.server, instance, bundle.attachment,
                            new ProgressListener(bundle.getPartName(), this.view)).toObservable();
                })
                .flatMap((Function<OpenRosaPartResponse, ObservableSource<OpenRosaPartResponse>>) response -> {
                    // set form and attachments statuses
                    setPartSuccessSubmissionStatuses(instance, response.getPartName());
                    return rxSaveFormInstance(instance, response, null);
                })
                .onErrorResumeNext(throwable -> {
                    setErrorSubmissionStatuses(instance, startStatus, throwable);
                    return rxSaveFormInstance(instance, null, throwable);
                })
                .doFinally(() -> view.hideFormSubmitLoading())
                .subscribe(
                        response -> view.formPartSubmitSuccess(instance, response),
                        throwable -> {
                            if (throwable instanceof NoConnectivityException) {
                                // PendingFormSendJob.scheduleJob();
                                view.formSubmitNoConnectivity();
                            } else {
                                Timber.e(throwable);//TODO Crahslytics removed
                                view.formPartSubmitError(throwable);
                            }
                        },
                        () -> view.formPartsSubmitEnded(instance)
                )
        );
    }

    // todo: move to FormSaver
    @Override
    public void saveForLaterFormInstance(String name) {
        CollectFormInstance instance = FormController.getActive().getCollectFormInstance();

        if (instance.getId() > 0) { // already saved
            view.saveForLaterFormInstanceSuccess();
            return;
        }

        if (!TextUtils.isEmpty(name)) {
            instance.setInstanceName(name);
        }

        disposables.add(keyDataSource.getDataSource()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .flatMapSingle((Function<DataSource, SingleSource<CollectFormInstance>>) dataSource
                        -> finalizeAndSaveFormInstance(dataSource, instance))
                .subscribe((collectServer) -> view.saveForLaterFormInstanceSuccess(),
                        throwable -> view.saveForLaterFormInstanceError(throwable)
                )
        );
    }

    @Override
    public boolean isSubmitting() {
        return disposables.size() > 0;
    }

    @Override
    public void userStopSubmission() {
        stopSubmission();
        view.submissionStoppedByUser();
    }

    @Override
    public void stopSubmission() {
        disposables.clear();
    }

    @Override
    public void destroy() {
        disposables.dispose();
        view = null;
    }

    private Single<CollectServer> finalizeFormInstance(DataSource dataSource, CollectFormInstance instance) {
        // finalize form (FormDef & CollectFormInstance)
        instance.getFormDef().postProcessInstance();

        if (instance.getStatus() == CollectFormInstanceStatus.UNKNOWN || instance.getStatus() == CollectFormInstanceStatus.DRAFT) {
            instance.setStatus(CollectFormInstanceStatus.FINALIZED);
        }

        return dataSource.saveInstance(instance).flatMap(
                (Function<CollectFormInstance, SingleSource<CollectServer>>) instance1 ->
                        dataSource.getCollectServer(instance1.getServerId())
        );
    }

    private Single<CollectFormInstance> finalizeAndSaveFormInstance(DataSource dataSource, CollectFormInstance instance) {
        // finalize form (FormDef & CollectFormInstance)
        instance.getFormDef().postProcessInstance();
        instance.setStatus(CollectFormInstanceStatus.SUBMISSION_PENDING);

        return dataSource.saveInstance(instance);
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

    private Observable<List<GranularSubmissionBundle>> createPartBundles(CollectFormInstance instance, NegotiatedCollectServer server) {
        List<GranularSubmissionBundle> bundles = new ArrayList<>();

        // we're adding this separately for simpler UI (having parts in order), if form is not already submitted partially or fully
        if (instance.getStatus() != CollectFormInstanceStatus.SUBMISSION_PARTIAL_PARTS &&
                instance.getStatus() != CollectFormInstanceStatus.SUBMITTED) {
            bundles.add(new GranularSubmissionBundle(server));
        }

        for (FormMediaFile attachment : instance.getWidgetMediaFiles()) {
            if (attachment.uploading && attachment.status != FormMediaFileStatus.SUBMITTED) {
                bundles.add(new GranularSubmissionBundle(server, attachment));
            }
        }

        return Observable.just(bundles);
    }

    private <T> Observable<T> rxSaveFormInstance(final CollectFormInstance instance, final T value, @Nullable final Throwable throwable) {
        return keyDataSource.getDataSource().flatMap((Function<DataSource, ObservableSource<T>>) dataSource ->
                dataSource.saveInstance(instance)
                        .toObservable()
                        .flatMap((Function<CollectFormInstance, ObservableSource<T>>) instance1 ->
                                throwable == null ? Observable.just(value) : Observable.error(throwable)));
    }

    private void setSuccessSubmissionStatuses(CollectFormInstance instance) {
        CollectFormInstanceStatus status = CollectFormInstanceStatus.SUBMITTED;

        for (FormMediaFile mediaFile : instance.getWidgetMediaFiles()) {
            if (mediaFile.uploading) {
                mediaFile.status = FormMediaFileStatus.SUBMITTED;
            } else {
                mediaFile.status = FormMediaFileStatus.NOT_SUBMITTED;
                status = CollectFormInstanceStatus.SUBMISSION_PARTIAL_PARTS;
            }
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

    /* @SuppressWarnings("MethodOnlyUsedFromInnerClass")
    private void updateMediaFilesQueue(@NonNull Collection<MediaFile> attachments) {
        if (attachments.size() == 0) {
            return;
        }

        RawMediaFileQueue queue = MyApplication.mediaFileQueue();

        if (queue == null) {
            return;
        }

        for (MediaFile mediaFile: attachments) {
            queue.addAndStartUpload(mediaFile);
        }
    } */

    private static class OfflineModeException extends Exception {
    }

    private static class GranularSubmissionBundle {
        NegotiatedCollectServer server;
        FormMediaFile attachment;

        GranularSubmissionBundle(NegotiatedCollectServer server) {
            this.server = server;
        }

        GranularSubmissionBundle(NegotiatedCollectServer server, FormMediaFile attachment) {
            this.server = server;
            this.attachment = attachment;
        }

        String getPartName() {
            return attachment != null ? attachment.getPartName() : C.OPEN_ROSA_XML_PART_NAME;
        }
    }

    // Ugly, think about elegant Rx/Flowable solution
    static class ProgressListener implements IProgressListener {
        private static final long REFRESH_TIME_MS = 500;
        private String partName;
        private IFormSubmitterContract.IView view;
        private long time;

        ProgressListener(String partName, IFormSubmitterContract.IView view) {
            this.partName = partName;
            this.view = view;
        }

        @Override
        public void onProgressUpdate(long current, long total) {
            long now = Util.currentTimestamp();

            if (view != null && now - time > REFRESH_TIME_MS) {
                time = now;
                ThreadUtil.runOnMain(() -> view.formPartUploadProgress(partName, (float) current / (float) total));
            }
        }
    }
}
