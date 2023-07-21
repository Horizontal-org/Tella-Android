package rs.readahead.washington.mobile.views.activity

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.viewModels
import org.hzontal.shared_ui.bottomsheet.BottomSheetUtils.showStandardSheet
import rs.readahead.washington.mobile.MyApplication
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.bus.event.CollectFormSubmissionErrorEvent
import rs.readahead.washington.mobile.bus.event.CollectFormSubmitStoppedEvent
import rs.readahead.washington.mobile.bus.event.CollectFormSubmittedEvent
import rs.readahead.washington.mobile.databinding.ActivityFormSubmitBinding
import rs.readahead.washington.mobile.databinding.ContentFormSubmitBinding
import rs.readahead.washington.mobile.domain.entity.collect.CollectFormInstance
import rs.readahead.washington.mobile.domain.entity.collect.CollectFormInstanceStatus
import rs.readahead.washington.mobile.domain.entity.collect.OpenRosaPartResponse
import rs.readahead.washington.mobile.javarosa.FormReSubmitter
import rs.readahead.washington.mobile.javarosa.FormUtils
import rs.readahead.washington.mobile.javarosa.IFormReSubmitterContract
import rs.readahead.washington.mobile.views.base_ui.BaseLockActivity
import rs.readahead.washington.mobile.views.collect.CollectFormEndView
import rs.readahead.washington.mobile.views.fragment.forms.SharedFormsViewModel

class FormSubmitActivity : BaseLockActivity(), IFormReSubmitterContract.IView {
    var endView: CollectFormEndView? = null
    private var formReSubmitter: FormReSubmitter? = null
    private var instance: CollectFormInstance? = null
    private lateinit var binding: ActivityFormSubmitBinding
    private lateinit var content: ContentFormSubmitBinding
    private val viewModel: SharedFormsViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        binding = ActivityFormSubmitBinding.inflate(layoutInflater)
        setContentView(binding.root)
        content = binding.content
        init()
        formReSubmitter = FormReSubmitter(this)
        setSupportActionBar(binding.toolbar)
        val actionBar = supportActionBar
        actionBar?.setDisplayHomeAsUpEnabled(true)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            findViewById<View>(R.id.appbar).outlineProvider = null
        } else {
            findViewById<View>(R.id.appbar).bringToFront()
        }
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
            if (formReSubmitter != null && formReSubmitter!!.isReSubmitting) {
                showStandardSheet(
                    this.supportFragmentManager,
                    getString(R.string.Collect_DialogTitle_StopExit),
                    getString(R.string.Collect_DialogExpl_ExitingStopSubmission),
                    getString(R.string.Collect_DialogAction_KeepSubmitting),
                    getString(R.string.Collect_DialogAction_StopAndExit),
                    null
                ) { onDialogBackPressed() }

                /*DialogsUtil.showExitWithSubmitDialog(this,
                        (dialog, which) -> finish(),
                        (dialog, which) -> {
                        });*/
            } else {
                finish()
            }
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        if (formReSubmitter != null && formReSubmitter!!.isReSubmitting) {
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
        MyApplication.bus().post(CollectFormSubmitStoppedEvent())
        super.onBackPressed()
        return
    }

    override fun onPause() {
        super.onPause()
        if (formReSubmitter != null && formReSubmitter!!.isReSubmitting) {
            formReSubmitter!!.stopReSubmission()
            submissionStoppedByUser()
        }
    }

    override fun onDestroy() {
        stopFormReSubmitter()
        super.onDestroy()
    }

    private fun init() {
        content.submitButton.setOnClickListener { view: View? ->
            onSubmitClick(
                view
            )
        }
        content.cancelButton.setOnClickListener { view: View? ->
            onCancelClick(
                view
            )
        }
        content.stopButton.setOnClickListener { view: View? -> onStopClick(view) }

        viewModel.collectFormInstance.observe(this, { instance ->
            if (instance != null) {
                onGetFormInstanceSuccess(instance)
            }
        })

        viewModel.onError.observe(this, { throwable ->
            onGetFormInstanceError(throwable)
        })

    }

    fun onSubmitClick(view: View?) {
        if (formReSubmitter != null) {
            formReSubmitter!!.reSubmitFormInstanceGranular(instance)
            hideFormSubmitButton()
            hideFormCancelButton()
            showFormStopButton()
        }
    }

    fun onCancelClick(view: View?) {
        onBackPressed()
        /*if (formReSubmitter != null) {
            formReSubmitter.userStopReSubmission();
        }*/
    }

    fun onStopClick(view: View?) {
        //onBackPressed();
        if (formReSubmitter != null) {
            formReSubmitter!!.userStopReSubmission()
        }
        MyApplication.bus().post(CollectFormSubmitStoppedEvent())
    }

    override fun formReSubmitError(error: Throwable) {
        val errorMessage = FormUtils.getFormSubmitErrorMessage(this, error)
        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
        MyApplication.bus().post(CollectFormSubmissionErrorEvent())
        finish()
    }

    override fun formReSubmitNoConnectivity() {
        Toast.makeText(
            this,
            R.string.collect_end_toast_notification_form_not_sent_no_connection,
            Toast.LENGTH_LONG
        ).show()
        MyApplication.bus().post(CollectFormSubmissionErrorEvent())
        finish()
    }

    override fun showReFormSubmitLoading(instance: CollectFormInstance) {
        invalidateOptionsMenu()
        hideFormSubmitButton()
        showFormCancelButton()
        disableScreenTimeout()
        if (endView != null) {
            endView!!.clearPartsProgress(instance)
        }
    }

    override fun hideReFormSubmitLoading() {
        enableScreenTimeout()
        invalidateOptionsMenu()
    }

    override fun formPartResubmitStart(instance: CollectFormInstance, partName: String) {
        if (endView != null) {
            runOnUiThread { endView!!.showUploadProgress(partName) }
        }
    }

    override fun formPartUploadProgress(partName: String, pct: Float) {
        if (endView != null) {
            runOnUiThread { endView!!.setUploadProgress(partName, pct) }
        }
    }

    override fun formPartResubmitSuccess(
        instance: CollectFormInstance,
        response: OpenRosaPartResponse
    ) {
        if (endView != null) {
            runOnUiThread { endView!!.hideUploadProgress(response.partName) }
        }
    }

    override fun formPartReSubmitError(error: Throwable) {
        formReSubmitError(error)
    }

    override fun formPartsResubmitEnded(instance: CollectFormInstance) {
        Toast.makeText(this, getString(R.string.collect_toast_form_submitted), Toast.LENGTH_LONG)
            .show()
        MyApplication.bus().post(CollectFormSubmittedEvent())
        finish()
    }

    override fun submissionStoppedByUser() {
        showFormEndView(false)
        showFormSubmitButton()
        onBackPressed()
        //hideFormCancelButton();
    }

    fun onGetFormInstanceSuccess(instance: CollectFormInstance) {
        this.instance = instance
        showFormEndView(false)
    }

    fun onGetFormInstanceError(throwable: Throwable) {
        Toast.makeText(this, R.string.collect_toast_fail_loading_form_instance, Toast.LENGTH_LONG)
            .show()
        finish()
    }

    override fun getContext(): Context {
        return this
    }

    private fun showFormEndView(offline: Boolean) {
        endView = CollectFormEndView(
            this,
            if (instance!!.status == CollectFormInstanceStatus.SUBMITTED) R.string.collect_end_heading_confirmation_form_submitted else R.string.collect_end_action_submit
        )
        endView!!.setInstance(instance!!, offline)
        content.formDetailsContainer.removeAllViews()
        content.formDetailsContainer.addView(endView!!)
        updateFormSubmitButton(false)
    }

    private fun enableMenuItems(menu: Menu) {
        val disabled = formReSubmitter != null && formReSubmitter!!.isReSubmitting
        for (i in 0 until menu.size()) {
            menu.getItem(i).isEnabled = !disabled
        }
    }

    private fun updateFormSubmitButton(offline: Boolean) {
        if (instance!!.status != CollectFormInstanceStatus.SUBMITTED) {
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
        content.submitButton.visibility = View.INVISIBLE
        content.submitButton.isClickable = false
    }

    private fun showFormSubmitButton() {
        content.submitButton.visibility = View.VISIBLE
        content.submitButton.isClickable = true
    }

    private fun stopFormReSubmitter() {
        if (formReSubmitter != null) {
            formReSubmitter!!.destroy()
            formReSubmitter = null
        }
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