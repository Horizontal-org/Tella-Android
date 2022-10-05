package rs.readahead.washington.mobile.views.activity;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;

import org.hzontal.shared_ui.bottomsheet.BottomSheetUtils;

import kotlin.Unit;
import rs.readahead.washington.mobile.MyApplication;
import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.bus.event.CollectFormSubmissionErrorEvent;
import rs.readahead.washington.mobile.bus.event.CollectFormSubmitStoppedEvent;
import rs.readahead.washington.mobile.bus.event.CollectFormSubmittedEvent;
import rs.readahead.washington.mobile.databinding.ActivityFormSubmitBinding;
import rs.readahead.washington.mobile.databinding.ContentFormSubmitBinding;
import rs.readahead.washington.mobile.domain.entity.collect.CollectFormInstance;
import rs.readahead.washington.mobile.domain.entity.collect.CollectFormInstanceStatus;
import rs.readahead.washington.mobile.domain.entity.collect.OpenRosaPartResponse;
import rs.readahead.washington.mobile.javarosa.FormReSubmitter;
import rs.readahead.washington.mobile.javarosa.FormUtils;
import rs.readahead.washington.mobile.javarosa.IFormReSubmitterContract;
import rs.readahead.washington.mobile.mvp.contract.IFormSubmitPresenterContract;
import rs.readahead.washington.mobile.mvp.presenter.FormSubmitPresenter;
import rs.readahead.washington.mobile.views.base_ui.BaseLockActivity;
import rs.readahead.washington.mobile.views.collect.CollectFormEndView;


public class FormSubmitActivity extends BaseLockActivity implements
        IFormReSubmitterContract.IView,
        IFormSubmitPresenterContract.IView {
    public static final String FORM_INSTANCE_ID_KEY = "fid";

    CollectFormEndView endView;

    private FormSubmitPresenter presenter;
    private FormReSubmitter formReSubmitter;

    private CollectFormInstance instance;
    private ActivityFormSubmitBinding binding;
    private ContentFormSubmitBinding content;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        binding = ActivityFormSubmitBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        content = binding.content;
        initListeners();

        formReSubmitter = new FormReSubmitter(this);

        setSupportActionBar(binding.toolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            findViewById(R.id.appbar).setOutlineProvider(null);
        } else {
            findViewById(R.id.appbar).bringToFront();
        }

        if (getIntent().hasExtra(FORM_INSTANCE_ID_KEY)) {
            long instanceId = getIntent().getLongExtra(FORM_INSTANCE_ID_KEY, 0);

            presenter = new FormSubmitPresenter(this);
            presenter.getFormInstance(instanceId);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.form_submit_menu, menu);
        enableMenuItems(menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            if (formReSubmitter != null && formReSubmitter.isReSubmitting()) {
                BottomSheetUtils.showStandardSheet(
                        this.getSupportFragmentManager(),
                        getString(R.string.Collect_DialogTitle_StopExit),
                        getString(R.string.Collect_DialogExpl_ExitingStopSubmission),
                        getString(R.string.Collect_DialogAction_KeepSubmitting),
                        getString(R.string.Collect_DialogAction_StopAndExit),
                        null, this::onDialogBackPressed);

                    /*DialogsUtil.showExitWithSubmitDialog(this,
                            (dialog, which) -> finish(),
                            (dialog, which) -> {
                            });*/
            } else {
                finish();
            }
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (formReSubmitter != null && formReSubmitter.isReSubmitting()) {
            BottomSheetUtils.showStandardSheet(
                    this.getSupportFragmentManager(),
                    getString(R.string.Collect_DialogTitle_StopExit),
                    getString(R.string.Collect_DialogExpl_ExitingStopSubmission),
                    getString(R.string.Collect_DialogAction_StopAndExit),
                    getString(R.string.Collect_DialogAction_KeepSubmitting),
                    this::onDialogBackPressed, null);
        } else {
            super.onBackPressed();
        }
        finish();
    }

    private Unit onDialogBackPressed() {
        MyApplication.bus().post(new CollectFormSubmitStoppedEvent());
        super.onBackPressed();
        return Unit.INSTANCE;
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (formReSubmitter != null && formReSubmitter.isReSubmitting()) {
            formReSubmitter.stopReSubmission();
            submissionStoppedByUser();
        }
    }

    @Override
    protected void onDestroy() {
        stopPresenter();
        stopFormReSubmitter();

        super.onDestroy();
    }

    private void initListeners() {
        content.submitButton.setOnClickListener(this::onSubmitClick);
        content.cancelButton.setOnClickListener(this::onCancelClick);
        content.stopButton.setOnClickListener(this::onStopClick);
    }

    public void onSubmitClick(View view) {
        if (formReSubmitter != null) {
            formReSubmitter.reSubmitFormInstanceGranular(instance);
            hideFormSubmitButton();
            hideFormCancelButton();
            showFormStopButton();
        }
    }

    public void onCancelClick(View view) {
        onBackPressed();
        /*if (formReSubmitter != null) {
            formReSubmitter.userStopReSubmission();
        }*/
    }

    public void onStopClick(View view) {
        //onBackPressed();
        if (formReSubmitter != null) {
            formReSubmitter.userStopReSubmission();
        }
        MyApplication.bus().post(new CollectFormSubmitStoppedEvent());
    }

    @Override
    public void formReSubmitError(Throwable error) {
        String errorMessage = FormUtils.getFormSubmitErrorMessage(this, error);
        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
        MyApplication.bus().post(new CollectFormSubmissionErrorEvent());
        finish();
    }

    @Override
    public void formReSubmitNoConnectivity() {
        Toast.makeText(this, R.string.collect_end_toast_notification_form_not_sent_no_connection, Toast.LENGTH_LONG).show();
        MyApplication.bus().post(new CollectFormSubmissionErrorEvent());
        finish();
    }

    @Override
    public void showReFormSubmitLoading(CollectFormInstance instance) {
        invalidateOptionsMenu();
        hideFormSubmitButton();
        showFormCancelButton();
        disableScreenTimeout();

        if (endView != null) {
            endView.clearPartsProgress(instance);
        }
    }

    @Override
    public void hideReFormSubmitLoading() {
        enableScreenTimeout();
        invalidateOptionsMenu();
    }

    @Override
    public void formPartResubmitStart(CollectFormInstance instance, String partName) {
        if (endView != null) {
            runOnUiThread(() -> endView.showUploadProgress(partName));
        }
    }

    @Override
    public void formPartUploadProgress(String partName, float pct) {
        if (endView != null) {
            runOnUiThread(() -> endView.setUploadProgress(partName, pct));
        }
    }

    @Override
    public void formPartResubmitSuccess(CollectFormInstance instance, OpenRosaPartResponse response) {
        if (endView != null) {
            runOnUiThread(() -> endView.hideUploadProgress(response.getPartName()));
        }
    }

    @Override
    public void formPartReSubmitError(Throwable error) {
        formReSubmitError(error);
    }

    @Override
    public void formPartsResubmitEnded(CollectFormInstance instance) {
        Toast.makeText(this, getString(R.string.collect_toast_form_submitted), Toast.LENGTH_LONG).show();
        MyApplication.bus().post(new CollectFormSubmittedEvent());
        finish();
    }

    @Override
    public void submissionStoppedByUser() {
        showFormEndView(false);
        showFormSubmitButton();
        onBackPressed();
        //hideFormCancelButton();
    }

    @Override
    public void onGetFormInstanceSuccess(CollectFormInstance instance) {
        this.instance = instance;
        showFormEndView(false);
    }

    @Override
    public void onGetFormInstanceError(Throwable throwable) {
        Toast.makeText(this, R.string.collect_toast_fail_loading_form_instance, Toast.LENGTH_LONG).show();
        finish();
    }

    @Override
    public Context getContext() {
        return this;
    }

    private void showFormEndView(boolean offline) {
        endView = new CollectFormEndView(this,
                instance.getStatus() == CollectFormInstanceStatus.SUBMITTED ? R.string.collect_end_heading_confirmation_form_submitted : R.string.collect_end_action_submit);
        endView.setInstance(this.instance, offline);
        content.formDetailsContainer.removeAllViews();
        content.formDetailsContainer.addView(endView);

        updateFormSubmitButton(false);
    }

    private void enableMenuItems(Menu menu) {
        boolean disabled = formReSubmitter != null && formReSubmitter.isReSubmitting();

        for (int i = 0; i < menu.size(); i++) {
            menu.getItem(i).setEnabled(!disabled);
        }
    }

    private void updateFormSubmitButton(boolean offline) {
        if (instance.getStatus() != CollectFormInstanceStatus.SUBMITTED) {
            content.submitButton.setVisibility(View.VISIBLE);
            //submitButton.setOffline(offline);
        }
    }

    private void showFormCancelButton() {
        content.cancelButton.setVisibility(View.VISIBLE);
    }

    private void hideFormCancelButton() {
        content.cancelButton.setVisibility(View.GONE);
    }

    private void showFormStopButton() {
        content.stopButton.setVisibility(View.VISIBLE);
    }

    private void hideFormSubmitButton() {
        content.submitButton.setVisibility(View.INVISIBLE);
        content.submitButton.setClickable(false);
    }

    private void showFormSubmitButton() {
        content.submitButton.setVisibility(View.VISIBLE);
        content.submitButton.setClickable(true);
    }

    private void stopPresenter() {
        if (presenter != null) {
            presenter.destroy();
            presenter = null;
        }
    }

    private void stopFormReSubmitter() {
        if (formReSubmitter != null) {
            formReSubmitter.destroy();
            formReSubmitter = null;
        }
    }

    private void disableScreenTimeout() {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    private void enableScreenTimeout() {
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }
}
