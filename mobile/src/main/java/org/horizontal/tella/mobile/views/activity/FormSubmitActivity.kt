package org.horizontal.tella.mobile.views.activity

import android.content.Context
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.viewModels
import dagger.hilt.android.AndroidEntryPoint
import org.hzontal.shared_ui.bottomsheet.BottomSheetUtils.showStandardSheet
import org.horizontal.tella.mobile.R
import org.horizontal.tella.mobile.databinding.ActivityFormSubmitBinding
import org.horizontal.tella.mobile.databinding.ContentFormSubmitBinding
import org.horizontal.tella.mobile.domain.entity.collect.CollectFormInstance
import org.horizontal.tella.mobile.domain.entity.collect.CollectFormInstanceStatus
import org.horizontal.tella.mobile.domain.entity.collect.OpenRosaPartResponse
import org.horizontal.tella.mobile.javarosa.FormUtils
import org.horizontal.tella.mobile.util.configureAppBar
import org.horizontal.tella.mobile.views.base_ui.BaseLockActivity
import org.horizontal.tella.mobile.views.collect.CollectFormEndView
import org.horizontal.tella.mobile.views.fragment.forms.SharedFormsViewModel
import org.horizontal.tella.mobile.views.fragment.forms.SubmitFormsViewModel
import org.horizontal.tella.mobile.views.fragment.forms.viewpager.OUTBOX_LIST_PAGE_INDEX
import org.horizontal.tella.mobile.views.fragment.uwazi.SharedLiveData
import org.horizontal.tella.mobile.views.fragment.uwazi.viewpager.SUBMITTED_LIST_PAGE_INDEX

@AndroidEntryPoint
class FormSubmitActivity : BaseLockActivity() {
    private var endView: CollectFormEndView? = null

    private lateinit var instance: CollectFormInstance
    private lateinit var binding: ActivityFormSubmitBinding
    private val content: ContentFormSubmitBinding by lazy { binding.content }
    private val viewModel: SharedFormsViewModel by viewModels()
    private val submitModel: SubmitFormsViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFormSubmitBinding.inflate(layoutInflater)
        setContentView(binding.root)

        init()

        binding.appbar.configureAppBar()

        if (intent.hasExtra(FORM_INSTANCE_ID_KEY)) {
            val instanceId = intent.getLongExtra(FORM_INSTANCE_ID_KEY, 0)
            viewModel.getFormInstance(instanceId)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.form_submit_menu, menu)
        enableMenuItems(menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == android.R.id.home) {
            if (submitModel.isReSubmitting()) {
                showStandardSheet(
                    this.supportFragmentManager,
                    getString(R.string.Collect_DialogTitle_StopExit),
                    getString(R.string.Collect_DialogExpl_ExitingStopSubmission),
                    getString(R.string.Collect_DialogAction_KeepSubmitting),
                    getString(R.string.Collect_DialogAction_StopAndExit),
                    null
                ) { onDialogBackPressed() }
            } else {
                finish()
            }
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (submitModel.isReSubmitting()) {
            showStandardSheet(
                this.supportFragmentManager,
                getString(R.string.Collect_DialogTitle_StopExit),
                getString(R.string.Collect_DialogExpl_ExitingStopSubmission),
                getString(R.string.Collect_DialogAction_StopAndExit),
                getString(R.string.Collect_DialogAction_KeepSubmitting),
                { onDialogBackPressed() },
                null
            )
        } else {
            super.onBackPressed()
        }
        finish()
    }

    private fun onDialogBackPressed() {
        SharedLiveData.updateViewPagerPosition.postValue(OUTBOX_LIST_PAGE_INDEX)
        super.onBackPressed()
        return
    }

    override fun onPause() {
        super.onPause()
        if (submitModel.isReSubmitting()) {
            submitModel.stopReSubmission()
            submissionStoppedByUser()
        }
    }

    private fun init() {
        content.submitButton.setOnClickListener { view: View? ->
            onSubmitClick()
        }
        content.cancelButton.setOnClickListener { view: View? ->
            onCancelClick()
        }
        content.stopButton.setOnClickListener { view: View? -> onStopClick() }

        initObservers()
        setSupportActionBar(binding.toolbar)
        val actionBar = supportActionBar
        actionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun initObservers() {
        with(viewModel) {
            collectFormInstance.observe(this@FormSubmitActivity) { instance ->
                if (instance != null) {
                    onGetFormInstanceSuccess(instance)
                }
            }
            onError.observe(this@FormSubmitActivity) { throwable ->
                onGetFormInstanceError()
            }
        }

        with(submitModel) {
            showReFormSubmitLoading.observe(this@FormSubmitActivity) { instance: CollectFormInstance ->
                showReFormSubmitLoading(instance)
            }

            formPartResubmitStart.observe(this@FormSubmitActivity) { (first, second): Pair<CollectFormInstance, String> ->
                formPartResubmitStart(first, second)
            }

            progressCallBack.observe(this@FormSubmitActivity) { (first, second): Pair<String, Float> ->
                formPartUploadProgress(first, second)
            }

            formPartResubmitSuccess.observe(this@FormSubmitActivity) { (first, second): Pair<CollectFormInstance, OpenRosaPartResponse?> ->
                divviupUtils.runODKSentEvent()
                second?.let { formPartResubmitSuccess(first, second) }
            }

            formReSubmitNoConnectivity.observe(this@FormSubmitActivity) { value: Boolean ->
                formReSubmitNoConnectivity()
            }

            formPartReSubmitError.observe(this@FormSubmitActivity) { throwable: Throwable? ->
                throwable?.let { formPartReSubmitError(throwable) }
            }

            hideReFormSubmitLoading.observe(this@FormSubmitActivity) { value: Boolean ->
                hideReFormSubmitLoading()
            }

            formPartsResubmitEnded.observe(this@FormSubmitActivity) { instance: CollectFormInstance ->
                formPartsResubmitEnded(instance)
            }

            submissionStoppedByUser.observe(this@FormSubmitActivity) { value: Boolean ->
                submissionStoppedByUser()
            }
        }
    }

    private fun onSubmitClick() {
        submitModel.reSubmitFormInstanceGranular(instance)
        hideFormSubmitButton()
        hideFormCancelButton()
        showFormStopButton()
    }

    private fun onCancelClick() {
        onBackPressed()
        submitModel.userStopReSubmission()
    }

    private fun onStopClick() {
        //onBackPressed();
        submitModel.userStopReSubmission()
        SharedLiveData.updateViewPagerPosition.postValue(OUTBOX_LIST_PAGE_INDEX)
    }

    private fun formReSubmitError(error: Throwable) {
        val errorMessage = FormUtils.getFormSubmitErrorMessage(this, error)
        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
        SharedLiveData.updateViewPagerPosition.postValue(OUTBOX_LIST_PAGE_INDEX)
        finish()
    }

    private fun formReSubmitNoConnectivity() {
        Toast.makeText(
            this,
            R.string.collect_end_toast_notification_form_not_sent_no_connection,
            Toast.LENGTH_LONG
        ).show()
        SharedLiveData.updateViewPagerPosition.postValue(OUTBOX_LIST_PAGE_INDEX)
        finish()
    }

    private fun showReFormSubmitLoading(instance: CollectFormInstance) {
        invalidateOptionsMenu()
        hideFormSubmitButton()
        showFormCancelButton()
        disableScreenTimeout()
        if (endView != null) {
            endView!!.clearPartsProgress(instance)
        }
    }

    private fun hideReFormSubmitLoading() {
        enableScreenTimeout()
        invalidateOptionsMenu()
    }

    private fun formPartResubmitStart(instance: CollectFormInstance, partName: String) {
        if (endView != null) {
            runOnUiThread { endView!!.showUploadProgress(partName) }
        }
    }

    private fun formPartUploadProgress(partName: String, pct: Float) {
        if (endView != null) {
            runOnUiThread { endView!!.setUploadProgress(partName, pct) }
        }
    }

    private fun formPartResubmitSuccess(
        instance: CollectFormInstance,
        response: OpenRosaPartResponse
    ) {
        if (endView != null) {
            runOnUiThread { endView!!.hideUploadProgress(response.partName) }
        }
    }

    private fun formPartReSubmitError(error: Throwable) {
        formReSubmitError(error)
    }

    private fun formPartsResubmitEnded(instance: CollectFormInstance) {
        Toast.makeText(this, getString(R.string.collect_toast_form_submitted), Toast.LENGTH_LONG)
            .show()
        SharedLiveData.updateViewPagerPosition.postValue(SUBMITTED_LIST_PAGE_INDEX)
        finish()
    }

    private fun submissionStoppedByUser() {
        showFormEndView(false)
        showFormSubmitButton()
        onBackPressed()
        //hideFormCancelButton();
    }

    private fun onGetFormInstanceSuccess(instance: CollectFormInstance) {
        this.instance = instance
        showFormEndView(false)
    }

    private fun onGetFormInstanceError() {
        Toast.makeText(this, R.string.collect_toast_fail_loading_form_instance, Toast.LENGTH_LONG)
            .show()
        finish()
    }

    private fun getContext(): Context {
        return this
    }

    private fun showFormEndView(offline: Boolean) {
        endView = CollectFormEndView(
            this,
            if (instance.status == CollectFormInstanceStatus.SUBMITTED) R.string.collect_end_heading_confirmation_form_submitted else R.string.collect_end_action_submit
        )
        endView!!.setInstance(instance, offline)
        content.formDetailsContainer.removeAllViews()
        content.formDetailsContainer.addView(endView!!)
        updateFormSubmitButton(false)
    }

    private fun enableMenuItems(menu: Menu) {
        val disabled = submitModel.isReSubmitting()
        for (i in 0 until menu.size()) {
            menu.getItem(i).isEnabled = !disabled
        }
    }

    private fun updateFormSubmitButton(offline: Boolean) {
        if (instance.status != CollectFormInstanceStatus.SUBMITTED) {
            content.submitButton.visibility = View.VISIBLE
            //submitButton.setOffline(offline);
        }
    }

    private fun showFormCancelButton() {
        content.cancelButton.visibility = View.VISIBLE
    }

    private fun hideFormCancelButton() {
        content.cancelButton.visibility = View.GONE
    }

    private fun showFormStopButton() {
        content.stopButton.visibility = View.VISIBLE
    }

    private fun hideFormSubmitButton() {
        content.submitButton.visibility = View.GONE
        content.submitButton.isClickable = false
    }

    private fun showFormSubmitButton() {
        content.submitButton.visibility = View.VISIBLE
        content.submitButton.isClickable = true
    }

    private fun disableScreenTimeout() {
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun enableScreenTimeout() {
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    companion object {
        const val FORM_INSTANCE_ID_KEY = "fid"
    }
}