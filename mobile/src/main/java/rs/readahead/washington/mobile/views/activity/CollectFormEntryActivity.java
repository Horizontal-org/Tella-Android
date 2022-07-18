package rs.readahead.washington.mobile.views.activity;

import static rs.readahead.washington.mobile.views.activity.CameraActivity.MEDIA_FILE_KEY;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import com.hzontal.tella_vault.MyLocation;
import com.hzontal.tella_vault.VaultFile;

import org.hzontal.shared_ui.bottomsheet.BottomSheetUtils;
import org.javarosa.core.model.FormIndex;
import org.javarosa.form.api.FormEntryCaption;
import org.javarosa.form.api.FormEntryPrompt;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import kotlin.Unit;
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
import rs.readahead.washington.mobile.mvp.contract.IQuestionAttachmentPresenterContract;
import rs.readahead.washington.mobile.mvp.presenter.QuestionAttachmentPresenter;
import rs.readahead.washington.mobile.util.C;
import rs.readahead.washington.mobile.util.DialogsUtil;
import rs.readahead.washington.mobile.util.PermissionUtil;
import rs.readahead.washington.mobile.util.Util;
import rs.readahead.washington.mobile.views.collect.CollectFormEndView;
import rs.readahead.washington.mobile.views.collect.CollectFormView;
import rs.readahead.washington.mobile.views.fragment.MicFragment;
import rs.readahead.washington.mobile.views.interfaces.ICollectEntryInterface;
import rs.readahead.washington.mobile.databinding.ActivityCollectFormEntryBinding;
import timber.log.Timber;


@RuntimePermissions
public class CollectFormEntryActivity extends MetadataActivity implements
        ICollectEntryInterface,
        IQuestionAttachmentPresenterContract.IView,
        IFormParserContract.IView,
        IFormSaverContract.IView,
        IFormSubmitterContract.IView {

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
    private MicFragment micFragment = null;
    private ActivityCollectFormEntryBinding binding;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityCollectFormEntryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        currentScreenView = null;
        //sectionIndex = 0;

        setSupportActionBar(binding.toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        upNavigationIcon = binding.toolbar.getNavigationIcon();
        setToolbarIcon();
        initForm();
        startPresenter();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            binding.appbar.setOutlineProvider(null);
        } else {
            binding.appbar.bringToFront();
        }

        binding.prevSection.setOnClickListener(v -> showPrevScreen());
        binding.nextSection.setOnClickListener(v -> showNextScreen());

        binding.submitButton.setOnClickListener(v -> {
            if (formSubmitter != null) {
                formSubmitter.submitActiveFormInstance(formTitle + " " + Util.getDateTimeString());
                hideToolbarIcon();
                hideSubmitButtons();
                showFormCancelButton();
            }
        });

        binding.cancelButton.setOnClickListener(v -> {
            if (userStopPresenterSubmission()) {
                hideFormCancelButton();
            }
        });

        endView = new CollectFormEndView(this, R.string.Uwazi_Submitted_Entity_Header_Title);

        disposables = MyApplication.bus().createCompositeDisposable();
        disposables.wire(FormAttachmentsUpdatedEvent.class, new EventObserver<FormAttachmentsUpdatedEvent>() {
            @Override
            public void onNext(@NonNull FormAttachmentsUpdatedEvent event) {
                formAttachmentsChanged();
            }
        });
        disposables.wire(LocationPermissionRequiredEvent.class, new EventObserver<LocationPermissionRequiredEvent>() {
            @Override
            public void onNext(@NonNull LocationPermissionRequiredEvent event) {
                CollectFormEntryActivityPermissionsDispatcher.startPermissionProcessWithPermissionCheck(CollectFormEntryActivity.this);
            }
        });
        disposables.wire(GPSProviderRequiredEvent.class, new EventObserver<GPSProviderRequiredEvent>() {
            @Override
            public void onNext(@NonNull GPSProviderRequiredEvent event) {
                CollectFormEntryActivityPermissionsDispatcher.startPermissionProcessWithPermissionCheck(CollectFormEntryActivity.this);
            }
        });
        disposables.wire(MediaFileBinaryWidgetCleared.class, new EventObserver<MediaFileBinaryWidgetCleared>() {
            @Override
            public void onNext(@NonNull MediaFileBinaryWidgetCleared event) {
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

        boolean forLater = formParser != null && (formParser.isFormFinal() || formParser.isFormEnd());

        menuItem = menu.findItem(R.id.saveFormMenuItem);
        menuItem.setVisible(!forLater);
        menuItem.setEnabled(!forLater);

        menuItem = menu.findItem(R.id.saveForLaterMenuItem);
        menuItem.setVisible(forLater);
        menuItem.setEnabled(forLater);

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

        return super.onOptionsItemSelected(item);
    }

    private void initForm(){
        formSaver = new FormSaver(this);
        formSubmitter = new FormSubmitter(this);
        formParser = new FormParser(this);
        formParser.parseForm();
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
                VaultFile vaultFile = (VaultFile) data.getSerializableExtra(MEDIA_FILE_KEY);
                putVaultFileInForm(vaultFile);
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

    private void putVaultFileInForm(VaultFile vaultFile){
        if (currentScreenView instanceof CollectFormView) {
            CollectFormView cfv = (CollectFormView) currentScreenView;

            if (vaultFile != null) {
                String filename = cfv.setBinaryData(vaultFile);

                if (filename != null) {
                    formParser.setWidgetMediaFile(filename, vaultFile);
                    formParser.setTellaMetadataFields(cfv, vaultFile.metadata);
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
        changeTemporaryTimeout();
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

    private void setToolbarIcon() {
        binding.toolbar.setEnabled(true);

        if (formParser != null && formParser.isFormEnd() && !formParser.isFormFinal()) {
            binding.toolbar.setNavigationIcon(upNavigationIcon);
        } else {
            binding.toolbar.setNavigationIcon(R.drawable.ic_close_white_24dp);
        }
    }

    private void hideToolbarIcon() {
        binding.toolbar.setEnabled(false);
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
            refreshFormEndView(false);
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
        binding = null;
    }

    /*
    @Override
    public void onCacheWordOpened() {
        super.onCacheWordOpened();
        formSaver = new FormSaver(this);
        formSubmitter = new FormSubmitter(this);
        formParser = new FormParser(this);
        formParser.parseForm();
    }*/

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
            BottomSheetUtils.showStandardSheet(
                    this.getSupportFragmentManager(),
                    getString(R.string.Collect_DialogTitle_StopExit),
                    getString(R.string.Collect_DialogExpl_ExitingStopSubmission),
                    getString(R.string.Collect_DialogAction_KeepSubmitting),
                    getString(R.string.Collect_DialogAction_StopAndExit),
                    null, this::onDialogBackPressed);
            /*alertDialog = DialogsUtil.showExitOnFinalDialog(this,
                    (dialog, which) -> onBackPressedWithoutCheck(),
                    (dialog, which) -> {
                    });*/
        } else {
            onBackPressedWithoutCheck();
        }
    }

    private Unit onDialogBackPressed() {
        onBackPressedWithoutCheck();
        return Unit.INSTANCE;
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
    public void formSubmitNoConnectivity() {
        Toast.makeText(getApplicationContext(), R.string.collect_end_toast_notification_form_not_sent_no_connection, Toast.LENGTH_LONG).show();
        MyApplication.bus().post(new CollectFormSubmittedEvent());
        finish();
    }

    @Override
    public void formPartSubmitStart(CollectFormInstance instance, String partName) {
        endView.showUploadProgress(partName);
        invalidateOptionsMenu();
        binding.toolbar.setNavigationIcon(R.drawable.ic_close_white_24dp);
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
        refreshFormEndView(false);
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
    public void onGetFilesSuccess(List<VaultFile> files) {

    }

    @Override
    public void onGetFilesError(Throwable error) {

    }

    @Override
    public void onMediaFileAdded(VaultFile vaultFile) {
        onActivityResult(C.MEDIA_FILE_ID, RESULT_OK, new Intent().putExtra(QuestionAttachmentActivity.MEDIA_FILE_KEY, vaultFile));
    }

    @Override
    public void onMediaFileAddError(Throwable error) {
        showToast(R.string.collect_toast_fail_attaching_file_to_form);
        Timber.d(error, getClass().getName());
    }

    @Override
    public void onMediaFileImported(VaultFile vaultFile) {
        onMediaFileAdded(vaultFile);
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

        alertDialog = new AlertDialog.Builder(this,R.style.PurpleBackgroundLightLettersDialogTheme)
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
            binding.screenFormView.removeView(currentScreenView);
        }
        currentScreenView = view;
        binding.screenFormView.addView(currentScreenView);
    }

    private void showFormView(CollectFormView view) {
        hideKeyboard();
        showScreenView(view);
        view.setFocus(this);
    }

    private void showFormEndView(CollectFormInstance instance) {
        hideKeyboard();
        showFormEndButtons();
        endView.setInstance(instance, false);
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
        hideFormCancelButton();
        hideSubmitButtons();
        hidePrevSectionButton();
    }

    private void setSectionButtons() {
        binding.buttonBottomLayout.setVisibility(View.VISIBLE);

        showNextSectionButton();

        if (formParser.isFirstScreen()) {
            setFirstSectionButtons();
            return;
        }

        binding.prevSection.setEnabled(true);
        binding.prevSection.setVisibility(View.VISIBLE);
    }

    private void setSubmitButtonText(boolean offline) {
        //submitButton.setOffline(offline);
    }

    private void showFormEndButtons() {
        setSubmitButtonText(Preferences.isOfflineMode());
        binding.submitButton.setEnabled(true);
        binding.submitButton.setVisibility(View.VISIBLE);
    }

    private void hidePrevSectionButton() {
        binding.prevSection.setEnabled(false);
        binding.prevSection.setVisibility(View.GONE);
    }

    private void hideSectionButtons() {
        hidePrevSectionButton();
        binding.nextSection.setEnabled(false);
        binding.nextSection.setVisibility(View.GONE);
    }

    private void hideSubmitButtons() {
        binding.submitButton.setEnabled(false);
        binding.submitButton.setVisibility(View.GONE);
    }

    private void showNextSectionButton() {
        binding.nextSection.setEnabled(true);
        binding.nextSection.setVisibility(View.VISIBLE);
    }

    private void showFormCancelButton() {
        binding.cancelButton.setEnabled(true);
        binding.cancelButton.setVisibility(View.VISIBLE);
    }

    private void hideFormCancelButton() {
        binding.cancelButton.setEnabled(false);
        binding.cancelButton.setVisibility(View.GONE);
    }

    private void showFormChangedDialog() {
        BottomSheetUtils.showStandardSheet(
                this.getSupportFragmentManager(),
                getString(R.string.collect_form_exit_unsaved_dialog_title),
                getString(R.string.collect_form_exit_dialog_expl),
                getString(R.string.collect_form_exit_dialog_action_save_exit),
                getString(R.string.collect_form_exit_dialog_action_exit_anyway),
                this::onSavePressed, this::onExitPressed);
/*
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
                });*/
    }

    private Unit onExitPressed() {
        onBackPressedWithoutCheck();
        return Unit.INSTANCE;
    }

    private Unit onSavePressed() {
        if (formSaver != null) {
            formSaver.saveActiveFormInstanceOnExit();
            onBackPressedWithoutCheck();
        } else {
            onBackPressedWithoutCheck();
        }
        return Unit.INSTANCE;
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

    @Override
    public void openAudioRecorder() {
        binding.entryLayout.setVisibility(View.GONE);
        micFragment = MicFragment.newInstance(true);
        addFragment(micFragment,R.id.rootCollectEntry);
    }

    @Override
    public void returnFileToForm(VaultFile file) {

        binding.entryLayout.setVisibility(View.VISIBLE);

        putVaultFileInForm(file);

        if (micFragment != null) {
            getSupportFragmentManager().beginTransaction().remove((Fragment) micFragment).commit();
        }
    }

    @Override
    public void stopWaitingForData() {
        binding.entryLayout.setVisibility(View.VISIBLE);
        formParser.stopWaitingBinaryData();
        saveCurrentScreen(false);
    }
}
