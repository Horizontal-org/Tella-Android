package rs.readahead.washington.mobile.views.activity;

import android.content.Context;
import android.os.Bundle;

import androidx.core.widget.NestedScrollView;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;

import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import rs.readahead.washington.mobile.MyApplication;
import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.bus.event.CollectFormSubmissionErrorEvent;
import rs.readahead.washington.mobile.bus.event.CollectFormSubmittedEvent;
import rs.readahead.washington.mobile.data.sharedpref.Preferences;
import rs.readahead.washington.mobile.domain.entity.collect.CollectFormInstance;
import rs.readahead.washington.mobile.domain.entity.collect.CollectFormInstanceStatus;
import rs.readahead.washington.mobile.domain.entity.collect.OpenRosaPartResponse;
import rs.readahead.washington.mobile.javarosa.FormReSubmitter;
import rs.readahead.washington.mobile.javarosa.FormUtils;
import rs.readahead.washington.mobile.javarosa.IFormReSubmitterContract;
import rs.readahead.washington.mobile.mvp.contract.IFormSubmitPresenterContract;
import rs.readahead.washington.mobile.mvp.presenter.FormSubmitPresenter;
import rs.readahead.washington.mobile.util.DialogsUtil;
import rs.readahead.washington.mobile.views.collect.CollectFormEndView;
import rs.readahead.washington.mobile.views.custom.FormSubmitButtonView;


public class FormSubmitActivity extends CacheWordSubscriberBaseActivity implements
        IFormReSubmitterContract.IView,
        IFormSubmitPresenterContract.IView {
    public static final String FORM_INSTANCE_ID_KEY = "fid";

    @BindView(R.id.formDetailsContainer)
    NestedScrollView endViewContainer;
    @BindView(R.id.submit_button)
    FormSubmitButtonView submitButton;
    @BindView(R.id.cancel_button)
    Button cancelButton;

    CollectFormEndView endView;

    private FormSubmitPresenter presenter;
    private FormReSubmitter formReSubmitter;

    private CollectFormInstance instance;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_form_submit);

        ButterKnife.bind(this);

        formReSubmitter = new FormReSubmitter(this);

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationIcon(R.drawable.ic_close_white_24dp);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
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
        setOfflineMenuIcon(menu.findItem(R.id.offlineMenuItem), Preferences.isOfflineMode());
        enableMenuItems(menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case android.R.id.home:
                if (formReSubmitter != null && formReSubmitter.isReSubmitting()) {
                    DialogsUtil.showExitWithSubmitDialog(this,
                            (dialog, which) -> finish(),
                            (dialog, which) -> {
                            });
                } else {
                    finish();
                }
                return true;

            case R.id.offlineMenuItem:
                DialogsUtil.showOfflineSwitchDialog(this, offline -> {
                    setOfflineMenuIcon(item, offline);
                    updateFormSubmitButton(offline);
                    refreshFormEndView(offline);
                });
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (formReSubmitter != null && formReSubmitter.isReSubmitting()) {
            DialogsUtil.showExitWithSubmitDialog(this,
                    (dialog, which) -> super.onBackPressed(),
                    (dialog, which) -> {
                    });
        } else {
            super.onBackPressed();
        }
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

    @OnClick(R.id.submit_button)
    public void onSubmitClick(View view) {
        if (formReSubmitter != null) {
            formReSubmitter.reSubmitFormInstanceGranular(instance);
            hideFormSubmitButton();
        }
    }

    @OnClick(R.id.cancel_button)
    public void onCancelClick(View view) {
        if (formReSubmitter != null) {
            formReSubmitter.userStopReSubmission();
        }
    }

    @Override
    public void formReSubmitError(Throwable error) {
        String errorMessage = FormUtils.getFormSubmitErrorMessage(this, error);
        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
        MyApplication.bus().post(new CollectFormSubmissionErrorEvent());
        finish();
    }

    @Override
    public void formResubmitOfflineMode() {
        Toast.makeText(this, R.string.collect_end_toast_saved_for_later, Toast.LENGTH_LONG).show();
        MyApplication.bus().post(new CollectFormSubmittedEvent());
        finish();
    }

    @Override
    public void formReSubmitNoConnectivity() {
        Toast.makeText(this, R.string.collect_end_toast_notification_form_not_sent_no_connection, Toast.LENGTH_LONG).show();
        MyApplication.bus().post(new CollectFormSubmittedEvent());
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
            endView.showUploadProgress(partName);
        }
    }

    @Override
    public void formPartUploadProgress(String partName, float pct) {
        if (endView != null) {
            endView.setUploadProgress(partName, pct);
        }
    }

    @Override
    public void formPartResubmitSuccess(CollectFormInstance instance, OpenRosaPartResponse response) {
        if (endView != null) {
            endView.hideUploadProgress(response.getPartName());
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
        showFormEndView(Preferences.isOfflineMode());
        showFormSubmitButton();
        hideFormCancelButton();
    }

    @Override
    public void onGetFormInstanceSuccess(CollectFormInstance instance) {
        this.instance = instance;
        showFormEndView(Preferences.isOfflineMode());
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
        endViewContainer.removeAllViews();
        endViewContainer.addView(endView);

        updateFormSubmitButton(Preferences.isOfflineMode());
    }

    private void refreshFormEndView(boolean offline) {
        if (endView != null) {
            endView.refreshInstance(offline);
        }
    }

    private void enableMenuItems(Menu menu) {
        boolean disabled = formReSubmitter != null && formReSubmitter.isReSubmitting();

        for (int i = 0; i < menu.size(); i++) {
            menu.getItem(i).setEnabled(!disabled);
        }
    }

    private void setOfflineMenuIcon(MenuItem menuItem, boolean offline) {
        menuItem.setIcon(offline ? R.drawable.ic_cloud_off_white_24dp : R.drawable.ic_cloud_queue_white_24dp);
    }

    private void updateFormSubmitButton(boolean offline) {
        if (instance.getStatus() != CollectFormInstanceStatus.SUBMITTED) {
            submitButton.setVisibility(View.VISIBLE);
            submitButton.setOffline(offline);
        }
    }

    private void showFormCancelButton() {
        cancelButton.setVisibility(View.VISIBLE);
    }

    private void hideFormCancelButton() {
        cancelButton.setVisibility(View.GONE);
    }

    private void hideFormSubmitButton() {
        submitButton.setVisibility(View.GONE);
    }

    private void showFormSubmitButton() {
        submitButton.setVisibility(View.VISIBLE);
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
