package rs.readahead.washington.mobile.views.activity;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;

import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import org.javarosa.core.model.FormIndex;
import org.javarosa.form.api.FormEntryCaption;
import org.javarosa.form.api.FormEntryPrompt;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import permissions.dispatcher.NeedsPermission;
import permissions.dispatcher.OnNeverAskAgain;
import permissions.dispatcher.OnPermissionDenied;
import permissions.dispatcher.OnShowRationale;
import permissions.dispatcher.PermissionRequest;
import permissions.dispatcher.RuntimePermissions;
import rs.readahead.washington.mobile.MyApplication;
import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.bus.EventCompositeDisposable;
import rs.readahead.washington.mobile.bus.EventObserver;
import rs.readahead.washington.mobile.bus.event.CollectFormInstanceDeletedEvent;
import rs.readahead.washington.mobile.bus.event.CollectFormSavedEvent;
import rs.readahead.washington.mobile.bus.event.CollectFormSubmissionErrorEvent;
import rs.readahead.washington.mobile.bus.event.CollectFormSubmitStoppedEvent;
import rs.readahead.washington.mobile.bus.event.CollectFormSubmittedEvent;
import rs.readahead.washington.mobile.bus.event.FormAttachmentsUpdatedEvent;
import rs.readahead.washington.mobile.bus.event.GPSProviderRequiredEvent;
import rs.readahead.washington.mobile.bus.event.LocationPermissionRequiredEvent;
import rs.readahead.washington.mobile.bus.event.MediaFileBinaryWidgetCleared;
import rs.readahead.washington.mobile.data.sharedpref.Preferences;
import rs.readahead.washington.mobile.domain.entity.MediaFile;
import rs.readahead.washington.mobile.domain.entity.MyLocation;
import rs.readahead.washington.mobile.domain.entity.collect.CollectFormInstance;
import rs.readahead.washington.mobile.domain.entity.collect.CollectFormInstanceStatus;
import rs.readahead.washington.mobile.domain.entity.collect.OpenRosaPartResponse;
import rs.readahead.washington.mobile.javarosa.FormParser;
import rs.readahead.washington.mobile.javarosa.FormSaver;
import rs.readahead.washington.mobile.javarosa.FormSubmitter;
import rs.readahead.washington.mobile.javarosa.FormUtils;
import rs.readahead.washington.mobile.javarosa.IFormParserContract;
import rs.readahead.washington.mobile.javarosa.IFormSaverContract;
import rs.readahead.washington.mobile.javarosa.IFormSubmitterContract;
import rs.readahead.washington.mobile.media.MediaFileBundle;
import rs.readahead.washington.mobile.mvp.contract.IQuestionAttachmentPresenterContract;
import rs.readahead.washington.mobile.mvp.presenter.QuestionAttachmentPresenter;
import rs.readahead.washington.mobile.util.C;
import rs.readahead.washington.mobile.util.DialogsUtil;
import rs.readahead.washington.mobile.util.PermissionUtil;
import rs.readahead.washington.mobile.util.Util;
import rs.readahead.washington.mobile.views.collect.CollectFormEndView;
import rs.readahead.washington.mobile.views.collect.CollectFormView;
import rs.readahead.washington.mobile.views.custom.FormSubmitButtonView;
import timber.log.Timber;


@RuntimePermissions
public class CollectFormEntryActivity extends MetadataActivity implements
        IQuestionAttachmentPresenterContract.IView,
        IFormParserContract.IView,
        IFormSaverContract.IView,
        IFormSubmitterContract.IView {
    @BindView(R.id.screenFormView)
    ViewGroup screenFormView;
    @BindView(R.id.prevSection)
    Button prevSectionButton;
    @BindView(R.id.nextSection)
    Button nextSectionButton;
    @BindView(R.id.submit_button)
    FormSubmitButtonView submitButton;
    @BindView(R.id.cancel_button)
    Button cancelButton;
    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.button_bottom_layout)
    ViewGroup buttonBottomLayout;

    private Drawable upNavigationIcon;

    private View currentScreenView;
    //private int sectionIndex;
    private String formTitle;
    private FormParser formParser;
    private FormSaver formSaver;
    private FormSubmitter formSubmitter;
    private EventCompositeDisposable disposables;
    private QuestionAttachmentPresenter presenter; // todo: use separate presenter just for importing, extract from this one

    private CollectFormEndView endView;
    private AlertDialog alertDialog;
    private ProgressDialog progressDialog;
    private boolean deleteEnabled = false;
    private boolean draftAutoSaved = false;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_collect_form_entry);
        ButterKnife.bind(this);

        currentScreenView = null;
        //sectionIndex = 0;

        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        upNavigationIcon = toolbar.getNavigationIcon();
        setToolbarIcon();

        startPresenter();

        prevSectionButton.setOnClickListener(v -> showPrevScreen());
        nextSectionButton.setOnClickListener(v -> showNextScreen());

        submitButton.setOnClickListener(v -> {
            if (formSubmitter != null) {
                formSubmitter.submitActiveFormInstance(formTitle + " " + Util.getDateTimeString());
                hideToolbarIcon();
                hideSubmitButtons();
                showFormCancelButton();
            }
        });

        cancelButton.setOnClickListener(v -> {
            if (userStopPresenterSubmission()) {
                hideFormCancelButton();
            }
        });

        endView = new CollectFormEndView(this, R.string.collect_end_heading);

        disposables = MyApplication.bus().createCompositeDisposable();
        disposables.wire(FormAttachmentsUpdatedEvent.class, new EventObserver<FormAttachmentsUpdatedEvent>() {
            @Override
            public void onNext(FormAttachmentsUpdatedEvent event) {
                formAttachmentsChanged();
            }
        });
        disposables.wire(LocationPermissionRequiredEvent.class, new EventObserver<LocationPermissionRequiredEvent>() {
            @Override
            public void onNext(LocationPermissionRequiredEvent event) {
                CollectFormEntryActivityPermissionsDispatcher.startPermissionProcessWithPermissionCheck(CollectFormEntryActivity.this);
            }
        });
        disposables.wire(GPSProviderRequiredEvent.class, new EventObserver<GPSProviderRequiredEvent>() {
            @Override
            public void onNext(GPSProviderRequiredEvent event) {
                CollectFormEntryActivityPermissionsDispatcher.startPermissionProcessWithPermissionCheck(CollectFormEntryActivity.this);
            }
        });
        disposables.wire(MediaFileBinaryWidgetCleared.class, new EventObserver<MediaFileBinaryWidgetCleared>() {
            @Override
            public void onNext(MediaFileBinaryWidgetCleared event) {
                if (formParser != null) {
                    formParser.removeWidgetMediaFile(event.filename);
                }
                clearedFormIndex(event.formIndex);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.collect_form_entry_menu, menu);

        if (hideMenuItems(menu)) {
            return true;
        }

        // turn menu items off if needed
        MenuItem menuItem;

        if (!deleteEnabled) {
            menuItem = menu.findItem(R.id.deleteFormMenuItem);
            menuItem.setEnabled(false);
            menuItem.setVisible(false);
        }

        boolean offline = Preferences.isOfflineMode();
        boolean forLater = formParser != null && (formParser.isFormFinal() || formParser.isFormEnd());

        setOfflineMenuIcon(menu.findItem(R.id.offlineMenuItem), offline);

        menuItem = menu.findItem(R.id.saveFormMenuItem);
        menuItem.setVisible(!forLater);
        menuItem.setEnabled(!forLater);

        menuItem = menu.findItem(R.id.saveForLaterMenuItem);
        menuItem.setVisible(forLater && !offline);
        menuItem.setEnabled(forLater && !offline);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.saveFormMenuItem) {
            if (formSaver != null) {
                saveCurrentScreen(false);
                formSaver.saveActiveFormInstance();
            }
            return true;
        }

        if (id == R.id.saveForLaterMenuItem) {
            if (formSubmitter != null) {
                formSubmitter.saveForLaterFormInstance(formTitle + " " + Util.getDateTimeString());
            }
            return true;
        }

        if (id == R.id.deleteFormMenuItem) {
            deleteFormInstance();
            return true;
        }

        if (id == android.R.id.home) {
            onBackPressed();
            return true;
        }

        if (id == R.id.offlineMenuItem) {
            alertDialog = DialogsUtil.showOfflineSwitchDialog(this, offline -> {
                setOfflineMenuIcon(item, offline);
                setSubmitButtonText(offline);
                refreshFormEndView(offline);
                invalidateOptionsMenu();
            });
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (!isLocationSettingsRequestCode(requestCode) && resultCode != RESULT_OK) {
            formParser.stopWaitingBinaryData(); // remove info about waiting index
            return;
        }

        switch (requestCode) {
            case C.MEDIA_FILE_ID:
                MediaFile mediaFile = (MediaFile) data.getSerializableExtra(QuestionAttachmentActivity.MEDIA_FILE_KEY);

                if (currentScreenView instanceof CollectFormView) {
                    CollectFormView cfv = (CollectFormView) currentScreenView;

                    if (mediaFile != null) {
                        String filename = cfv.setBinaryData(mediaFile);

                        if (filename != null) {
                            formParser.setWidgetMediaFile(filename, mediaFile);
                            formParser.setTellaMetadataFields(cfv, mediaFile.getMetadata());
                        } else {
                            Timber.e("Binary data not set on waiting widget");
                        }
                    } else {
                        formParser.removeWidgetMediaFile(cfv.clearBinaryData());
                        formParser.clearTellaMetadataFields(cfv);
                    }
                }

                formParser.stopWaitingBinaryData();
                saveCurrentScreen(false);
                break;

            case C.SELECTED_LOCATION:
                MyLocation myLocation = (MyLocation) data.getSerializableExtra(LocationMapActivity.SELECTED_LOCATION);

                if (currentScreenView instanceof CollectFormView) {
                    CollectFormView cfv = (CollectFormView) currentScreenView;

                    if (myLocation != null) {
                        cfv.setBinaryData(myLocation);
                    } else {
                        cfv.clearBinaryData();
                    }
                }

                formParser.stopWaitingBinaryData();
                saveCurrentScreen(false);
                break;

            case C.IMPORT_IMAGE:
                Uri image = data.getData();
                if (image != null) {
                    presenter.importImage(image);
                }
                break;

            case C.IMPORT_VIDEO:
                Uri video = data.getData();
                if (video != null) {
                    presenter.importVideo(video);
                }
                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        CollectFormEntryActivityPermissionsDispatcher.onRequestPermissionsResult(this, requestCode, grantResults);
    }

    @NeedsPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    public void startPermissionProcess() {
        manageLocationSettings(C.GPS_PROVIDER, () -> {
        });
    }

    @OnShowRationale(Manifest.permission.ACCESS_FINE_LOCATION)
    void showFineLocationRationale(final PermissionRequest request) {
        alertDialog = PermissionUtil.showRationale(this, request, getString(R.string.permission_dialog_expl_GPS));
    }

    @OnPermissionDenied(Manifest.permission.ACCESS_FINE_LOCATION)
    void onFineLocationPermissionDenied() {
    }

    @OnNeverAskAgain(Manifest.permission.ACCESS_FINE_LOCATION)
    void onFineLocationNeverAskAgain() {
    }

    private boolean hideMenuItems(Menu menu) {
        boolean submitting = isPresenterSubmitting();

        for (int i = 0; i < menu.size(); i++) {
            menu.getItem(i).setEnabled(!submitting);
            menu.getItem(i).setVisible(!submitting);
        }

        return submitting;
    }

    private void setOfflineMenuIcon(MenuItem menuItem, boolean offline) {
        menuItem.setIcon(offline ? R.drawable.ic_cloud_off_white_24dp : R.drawable.ic_cloud_queue_white_24dp);
    }

    private void setToolbarIcon() {
        toolbar.setEnabled(true);

        if (formParser != null && formParser.isFormEnd() && !formParser.isFormFinal()) {
            toolbar.setNavigationIcon(upNavigationIcon);
        } else {
            toolbar.setNavigationIcon(R.drawable.ic_close_white_24dp);
        }
    }

    private void hideToolbarIcon() {
        toolbar.setEnabled(false);
    }

    private void clearedFormIndex(FormIndex formIndex) {
        if (formParser != null) {
            if (currentScreenView instanceof CollectFormView) {
                CollectFormView cfv = (CollectFormView) currentScreenView;
                formParser.clearTellaMetadataFields(formIndex, cfv);
            }
        }
    }

    private boolean isLocationSettingsRequestCode(int requestCode) {
        return requestCode == C.GPS_PROVIDER;
    }

    private void deleteFormInstance() {
        if (formSaver == null) return;

        boolean cloned = formSaver.isActiveInstanceCloned();

        alertDialog = DialogsUtil.showFormInstanceDeleteDialog(
                this,
                cloned ? CollectFormInstanceStatus.SUBMITTED : CollectFormInstanceStatus.UNKNOWN,
                (dialog, which) -> {
                    if (formSaver != null) {
                        formSaver.deleteActiveFormInstance();
                    }
                });
    }

    @Override
    protected void onStart() {
        super.onStart();
        startLocationMetadataListening();
    }

    @Override
    protected void onPause() {
        super.onPause();

        closeAlertDialog();

        if (isPresenterSubmitting()) {
            stopPresenterSubmission();
            refreshFormEndView(Preferences.isOfflineMode());
            hideFormCancelButton();
            showFormEndButtons();
            invalidateOptionsMenu();
        }

        saveCurrentScreen(false);
    }

    @Override
    protected void onStop() {
        stopLocationMetadataListening();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        if (draftAutoSaved) {
            MyApplication.bus().post(new CollectFormSavedEvent());
        }

        if (disposables != null) {
            disposables.dispose();
        }

        destroyFormParser();
        destroyFormSubmitter();
        destroyFormSaver();
        destroyPresenter();

        super.onDestroy();
    }

    @Override
    public void onCacheWordOpened() {
        super.onCacheWordOpened();
        formSaver = new FormSaver(this);
        formSubmitter = new FormSubmitter(this);
        formParser = new FormParser(this);
        formParser.parseForm();
    }

    @Override
    public void onBackPressed() {
        if (isPresenterSubmitting()) {
            alertDialog = DialogsUtil.showExitWithSubmitDialog(this,
                    (dialog, which) -> {
                        stopPresenterSubmission();
                        onBackPressedNotSubmitting(true);
                    },
                    (dialog, which) -> {
                    });
        } else {
            onBackPressedNotSubmitting(false);
        }
    }

    private void onBackPressedNotSubmitting(boolean stopped) {
        if (formParser == null) {
            onBackPressedWithoutCheck();
            return;
        }

        if (formParser.isFormEnd() && !formParser.isFormFinal()) {
            hideFormCancelButton();
            hideSubmitButtons();
            showPrevScreen();
            invalidateOptionsMenu();
        } else if (formParser.isFormChanged() && !formParser.isFormFinal()) {
            showFormChangedDialog();
        } else if (formParser.isFormFinal() && !stopped) {
            alertDialog = DialogsUtil.showExitOnFinalDialog(this,
                    (dialog, which) -> onBackPressedWithoutCheck(),
                    (dialog, which) -> {
                    });
        } else {
            onBackPressedWithoutCheck();
        }
    }

    private void onBackPressedWithoutCheck() {
        if (draftAutoSaved) {
            MyApplication.bus().post(new CollectFormSavedEvent());
        }

        super.onBackPressed();
    }

    @Override
    public void formBeginning(String title) {
        formTitle = title;
        setTitle(formTitle);
        formParser.stepToNextScreen();
    }

    @Override
    public void formEnd(CollectFormInstance instance) {
        Util.hideKeyboard(this, endView);
        showFormEndView(instance);
        hideSectionButtons();
        setToolbarIcon();
        invalidateOptionsMenu();
    }

    @Override
    public void formQuestion(FormEntryPrompt[] prompts, FormEntryCaption[] groups) {
        showFormView(new CollectFormView(this, prompts, groups));
        setSectionButtons();
        setToolbarIcon();
    }

    @Override
    public void formGroup(FormEntryPrompt[] prompts, FormEntryCaption[] groups) {
        showFormView(new CollectFormView(this, prompts, groups));
        setSectionButtons();
        setToolbarIcon();
    }

    @Override
    public void formRepeat(FormEntryPrompt[] prompts, FormEntryCaption[] groups) {
        showFormView(new CollectFormView(this, prompts, groups));
        setSectionButtons();
        setToolbarIcon();
    }

    @Override
    public void formPromptNewRepeat(int lastRepeatCount, String groupText) {
        createPromptDialog(lastRepeatCount, groupText);
    }

    @Override
    public void formParseError(Throwable error) {
        showToast(R.string.collect_toast_fail_parsing_form);
    }

    @Override
    public void formSaveError(Throwable error) {
        Timber.d(error, getClass().getName());
    }

    @Override
    public void showSaveFormInstanceLoading() {
    }

    @Override
    public void hideSaveFormInstanceLoading() {
    }

    @Override
    public void showDeleteFormInstanceStart() {
    }

    @Override
    public void hideDeleteFormInstanceEnd() {
    }

    @Override
    public void formInstanceSaveError(Throwable throwable) {
    }

    @Override
    public void formInstanceSaveSuccess(CollectFormInstance instance) {
        Toast.makeText(this, getFormSaveMsg(instance), Toast.LENGTH_SHORT).show();
        formParser.startFormChangeTracking();
        MyApplication.bus().post(new CollectFormSavedEvent());
    }

    @Override
    public void formInstanceAutoSaveSuccess(CollectFormInstance instance) {
        Toast.makeText(this, getFormSaveMsg(instance), Toast.LENGTH_SHORT).show();
        formParser.startFormChangeTracking();
        draftAutoSaved = true;
    }

    private String getFormSaveMsg(CollectFormInstance instance) {
        switch (instance.getStatus()) {
            case UNKNOWN:
            case DRAFT:
            default:
                return getString(R.string.collect_toast_draft_saved);
        }
    }

    @Override
    public void formInstanceDeleteSuccess(boolean cloned) {
        MyApplication.bus().post(new CollectFormInstanceDeletedEvent(cloned));
        finish();
    }

    @Override
    public void formInstanceDeleteError(Throwable throwable) {
        Timber.d(throwable, getClass().getName());
    }

    @Override
    public void showFormSubmitLoading(CollectFormInstance instance) {
        invalidateOptionsMenu();
        endView.clearPartsProgress(instance);
        disableScreenTimeout();
    }

    @Override
    public void hideFormSubmitLoading() {
        setToolbarIcon();
        invalidateOptionsMenu();
        enableScreenTimeout();
    }

    @Override
    public void formSubmitError(Throwable error) {
        String errorMessage = FormUtils.getFormSubmitErrorMessage(this, error);

        Toast.makeText(getApplicationContext(), errorMessage, Toast.LENGTH_LONG).show();

        MyApplication.bus().post(new CollectFormSubmissionErrorEvent()); // refresh form lists..
        finish();
    }

    @Override
    public void formSubmitOfflineMode() {
        Toast.makeText(getApplicationContext(), R.string.collect_end_toast_saved_for_later, Toast.LENGTH_LONG).show();
        MyApplication.bus().post(new CollectFormSubmittedEvent());
        finish();
    }

    @Override
    public void formSubmitNoConnectivity() {
        Toast.makeText(getApplicationContext(), R.string.collect_end_toast_notification_form_not_sent_no_connection, Toast.LENGTH_LONG).show();
        MyApplication.bus().post(new CollectFormSubmittedEvent());
        finish();
    }

    /*@Override
    public void formSubmitSuccess(CollectFormInstance instance, OpenRosaResponse response) {
        String successMessage = FormUtils.getFormSubmitSuccessMessage(this, response);

        Toast.makeText(getApplicationContext(), successMessage, Toast.LENGTH_LONG).show();

        MyApplication.bus().post(new CollectFormSubmittedEvent());
        finish();
    }*/

    @Override
    public void formPartSubmitStart(CollectFormInstance instance, String partName) {
        endView.showUploadProgress(partName);
        invalidateOptionsMenu();
        toolbar.setNavigationIcon(R.drawable.ic_close_white_24dp);
    }

    @Override
    public void formPartUploadProgress(String partName, float pct) {
        endView.setUploadProgress(partName, pct);
    }

    @Override
    public void formPartSubmitSuccess(CollectFormInstance instance, OpenRosaPartResponse response) {
        endView.hideUploadProgress(response.getPartName());
    }

    @Override
    public void formPartSubmitError(Throwable error) {
        // error on part stops entire submission
        formSubmitError(error);
    }

    @Override
    public void formPartsSubmitEnded(CollectFormInstance instance) {
        Toast.makeText(getApplicationContext(), getString(R.string.collect_toast_form_submitted), Toast.LENGTH_LONG).show();
        MyApplication.bus().post(new CollectFormSubmittedEvent());
        finish();
    }

    @Override
    public void submissionStoppedByUser() {
        MyApplication.bus().post(new CollectFormSubmitStoppedEvent());
        refreshFormEndView(Preferences.isOfflineMode());
        hideFormCancelButton();
        showFormEndButtons();
    }

    @Override
    public void formConstraintViolation(FormIndex formIndex, String errorString) {
        if (currentScreenView instanceof CollectFormView) {
            ((CollectFormView) currentScreenView).setValidationConstraintText(formIndex, errorString);
            showToast(getString(R.string.collect_form_toast_validation_generic_error));
        }
    }

    @Override
    public void saveForLaterFormInstanceSuccess() {
        Toast.makeText(getApplicationContext(), R.string.collect_toast_form_saved_for_later_submission, Toast.LENGTH_LONG).show();
        MyApplication.bus().post(new CollectFormSubmittedEvent());
        finish();
    }

    @Override
    public void saveForLaterFormInstanceError(Throwable throwable) {
        Timber.d(throwable, getClass().getName());
    }

    @Override
    public void formPropertiesChecked(boolean enableDelete) {
        boolean invalidateOptionsMenu = false;

        if (enableDelete) {
            deleteEnabled = true;
            invalidateOptionsMenu = true;
        }

        if (invalidateOptionsMenu) {
            invalidateOptionsMenu();
        }
    }

    @Override
    public void onGetFilesStart() {

    }

    @Override
    public void onGetFilesEnd() {

    }

    @Override
    public void onGetFilesSuccess(List<MediaFile> files) {

    }

    @Override
    public void onGetFilesError(Throwable error) {

    }

    @Override
    public void onMediaFileAdded(MediaFile mediaFile) {
        onActivityResult(C.MEDIA_FILE_ID, RESULT_OK, new Intent().putExtra(QuestionAttachmentActivity.MEDIA_FILE_KEY, mediaFile));
    }

    @Override
    public void onMediaFileAddError(Throwable error) {
        showToast(R.string.collect_toast_fail_attaching_file_to_form);
        Timber.d(error, getClass().getName());
    }

    @Override
    public void onMediaFileImported(MediaFileBundle mediaFileBundle) {
        presenter.setAttachment(mediaFileBundle.getMediaFile());
        presenter.addNewMediaFile(mediaFileBundle);
    }

    @Override
    public void onImportError(Throwable error) {
        showToast(R.string.gallery_toast_fail_importing_file);
        Timber.d(error, getClass().getName());
    }

    @Override
    public void onImportStarted() {
        progressDialog = DialogsUtil.showProgressDialog(this, getString(R.string.gallery_dialog_expl_encrypting));
    }

    @Override
    public void onImportEnded() {
        hideProgressDialog();
        showToast(R.string.gallery_toast_file_encrypted);
    }

    @Override
    public void formSavedOnExit() {
        MyApplication.bus().post(new CollectFormSavedEvent());
        closeAlertDialog();
        onBackPressedWithoutCheck();
    }

    @Override
    public Context getContext() {
        return this;
    }

    private void closeAlertDialog() {
        if (alertDialog != null && alertDialog.isShowing()) {
            alertDialog.dismiss();
        }
    }

    private void createPromptDialog(int lastRepeatCount, String groupText) {
        if (alertDialog != null && alertDialog.isShowing()) {
            return;
        }

        alertDialog = new AlertDialog.Builder(this)
                .setPositiveButton(R.string.collect_form_dialog_action_add_group, (dialog, which) -> formParser.executeRepeat())
                .setNegativeButton(R.string.collect_form_dialog_action_dont_add_group, (dialog, which) -> formParser.cancelRepeat())
                .setCancelable(false)
                .create();

        if (lastRepeatCount > 0) {
            alertDialog.setTitle(getString(R.string.collect_form_dialog_title_add_additional_group, groupText));
            alertDialog.setMessage(getString(R.string.collect_form_dialog_expl_add_additional_group, groupText));
        } else {
            alertDialog.setTitle(getString(R.string.collect_form_dialog_action_add_first_group, groupText));
            alertDialog.setMessage(getString(R.string.collect_form_dialog_expl_add_first_group, groupText));
        }

        alertDialog.show();
    }

    @SuppressWarnings("MethodOnlyUsedFromInnerClass")
    private void formAttachmentsChanged() {
        invalidateOptionsMenu();
    }

    private void showScreenView(View view) {
        if (currentScreenView != null) {
            screenFormView.removeView(currentScreenView);
        }
        currentScreenView = view;
        screenFormView.addView(currentScreenView);
    }

    private void showFormView(CollectFormView view) {
        hideKeyboard();
        showScreenView(view);
        view.setFocus(this);
    }

    private void showFormEndView(CollectFormInstance instance) {
        hideKeyboard();
        showFormEndButtons();
        endView.setInstance(instance, Preferences.isOfflineMode());
        showScreenView(endView);
    }

    private void refreshFormEndView(boolean offline) {
        endView.refreshInstance(offline);
    }

    @SuppressWarnings("MethodOnlyUsedFromInnerClass")
    private void showNextScreen() {
        if (saveCurrentScreen(true)) {
            formSaver.autoSaveFormInstance();
            formParser.stepToNextScreen();
        }
    }

    private void showPrevScreen() {
        if (saveCurrentScreen(false)) {
            formSaver.autoSaveFormInstance();
            formParser.stepToPrevScreen();
        }
    }

    private boolean saveCurrentScreen(boolean checkConstraints) {
        if (currentScreenView instanceof CollectFormView) {
            CollectFormView cfv = (CollectFormView) currentScreenView;

            if (checkConstraints) {
                cfv.clearValidationConstraints();
            }

            return formSaver.saveScreenAnswers(cfv.getAnswers(), checkConstraints);
        }

        return true;
    }

    private void hideKeyboard() {
        View v = getCurrentFocus();
        if (v != null) {
            Util.hideKeyboard(this, v);
        }
    }

    private void destroyFormSaver() {
        if (formSaver != null) {
            formSaver.destroy();
            formSaver = null;
        }
    }

    private void destroyFormSubmitter() {
        if (formSubmitter != null) {
            formSubmitter.destroy();
            formSubmitter = null;
        }
    }

    private void destroyFormParser() {
        if (formParser != null) {
            formParser.destroy();
            formParser = null;
        }
    }

    // this bottom buttons on/off thing looks stupid :)
    private void setFirstSectionButtons() {
        hideSubmitButtons();
        hidePrevSectionButton();
    }

    private void setSectionButtons() {
        buttonBottomLayout.setVisibility(View.VISIBLE);

        showNextSectionButton();

        if (formParser.isFirstScreen()) {
            setFirstSectionButtons();
            return;
        }

        prevSectionButton.setEnabled(true);
        prevSectionButton.setVisibility(View.VISIBLE);
    }

    private void setSubmitButtonText(boolean offline) {
        submitButton.setOffline(offline);
    }

    private void showFormEndButtons() {
        setSubmitButtonText(Preferences.isOfflineMode());
        submitButton.setEnabled(true);
        submitButton.setVisibility(View.VISIBLE);
    }

    private void hidePrevSectionButton() {
        prevSectionButton.setEnabled(false);
        prevSectionButton.setVisibility(View.GONE);
    }

    private void hideSectionButtons() {
        hidePrevSectionButton();
        nextSectionButton.setEnabled(false);
        nextSectionButton.setVisibility(View.GONE);
    }

    private void hideSubmitButtons() {
        submitButton.setEnabled(false);
        submitButton.setVisibility(View.GONE);
    }

    private void showNextSectionButton() {
        nextSectionButton.setEnabled(true);
        nextSectionButton.setVisibility(View.VISIBLE);
    }

    private void showFormCancelButton() {
        cancelButton.setEnabled(true);
        cancelButton.setVisibility(View.VISIBLE);
    }

    private void hideFormCancelButton() {
        cancelButton.setEnabled(false);
        cancelButton.setVisibility(View.GONE);
    }

    private void showFormChangedDialog() {
        String message = getString(R.string.collect_form_exit_dialog_expl);

        alertDialog = DialogsUtil.showMessageOKCancelWithTitle(this,
                message,
                getString(R.string.collect_form_exit_unsaved_dialog_title),
                getString(R.string.collect_form_exit_dialog_action_save_exit),
                getString(R.string.collect_form_exit_dialog_action_exit_anyway),
                (dialog, which) -> {
                    if (formSaver != null) {
                        formSaver.saveActiveFormInstanceOnExit();
                        return;
                    }
                    dialog.dismiss();
                    onBackPressedWithoutCheck();
                },
                (dialog, which) -> {
                    dialog.dismiss();
                    onBackPressedWithoutCheck();
                });
    }

    private boolean isPresenterSubmitting() {
        return formSubmitter != null && formSubmitter.isSubmitting();
    }

    private void stopPresenterSubmission() {
        if (formSubmitter != null) {
            formSubmitter.stopSubmission();
            MyApplication.bus().post(new CollectFormSubmitStoppedEvent());
        }
    }

    private boolean userStopPresenterSubmission() {
        if (formSubmitter != null) {
            formSubmitter.userStopSubmission();
            return true;
        }

        return false;
    }

    private void startPresenter() {
        presenter = new QuestionAttachmentPresenter(this);
    }

    private void destroyPresenter() {
        if (presenter != null) {
            presenter.destroy();
            presenter = null;
        }
    }

    private void hideProgressDialog() {
        if (progressDialog != null) {
            progressDialog.dismiss();
            progressDialog = null;
        }
    }

    private void disableScreenTimeout() {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    private void enableScreenTimeout() {
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }
}
