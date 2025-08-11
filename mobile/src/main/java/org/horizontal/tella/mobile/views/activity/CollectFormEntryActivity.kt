package org.horizontal.tella.mobile.views.activity

import android.Manifest
import android.app.ProgressDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.hzontal.tella_vault.MyLocation
import com.hzontal.tella_vault.VaultFile
import dagger.hilt.android.AndroidEntryPoint
import org.horizontal.tella.mobile.MyApplication
import org.horizontal.tella.mobile.R
import org.horizontal.tella.mobile.bus.EventObserver
import org.horizontal.tella.mobile.bus.event.AudioRecordEvent
import org.horizontal.tella.mobile.bus.event.LocationPermissionRequiredEvent
import org.horizontal.tella.mobile.bus.event.MediaFileBinaryWidgetCleared
import org.horizontal.tella.mobile.databinding.ActivityCollectFormEntryBinding
import org.horizontal.tella.mobile.domain.entity.collect.CollectFormInstance
import org.horizontal.tella.mobile.domain.entity.collect.CollectFormInstanceStatus
import org.horizontal.tella.mobile.domain.entity.collect.OpenRosaPartResponse
import org.horizontal.tella.mobile.javarosa.FormParser
import org.horizontal.tella.mobile.javarosa.FormSaver
import org.horizontal.tella.mobile.javarosa.FormUtils
import org.horizontal.tella.mobile.javarosa.IFormParserContract
import org.horizontal.tella.mobile.javarosa.IFormSaverContract
import org.horizontal.tella.mobile.util.C
import org.horizontal.tella.mobile.util.DialogsUtil
import org.horizontal.tella.mobile.util.PermissionUtil.showRationale
import org.horizontal.tella.mobile.util.Util
import org.horizontal.tella.mobile.util.hide
import org.horizontal.tella.mobile.util.show
import org.horizontal.tella.mobile.views.activity.camera.CameraActivity
import org.horizontal.tella.mobile.views.activity.camera.CameraActivity.Companion.VAULT_CURRENT_ROOT_PARENT
import org.horizontal.tella.mobile.views.collect.CollectFormEndView
import org.horizontal.tella.mobile.views.collect.CollectFormView
import org.horizontal.tella.mobile.views.fragment.forms.QuestionAttachmentModel
import org.horizontal.tella.mobile.views.fragment.forms.SubmitFormsViewModel
import org.horizontal.tella.mobile.views.fragment.forms.viewpager.OUTBOX_LIST_PAGE_INDEX
import org.horizontal.tella.mobile.views.fragment.recorder.COLLECT_ENTRY
import org.horizontal.tella.mobile.views.fragment.recorder.MicActivity
import org.horizontal.tella.mobile.views.fragment.recorder.MicFragment
import org.horizontal.tella.mobile.views.fragment.uwazi.SharedLiveData
import org.horizontal.tella.mobile.views.fragment.uwazi.viewpager.DRAFT_LIST_PAGE_INDEX
import org.horizontal.tella.mobile.views.fragment.uwazi.viewpager.SUBMITTED_LIST_PAGE_INDEX
import org.horizontal.tella.mobile.views.interfaces.ICollectEntryInterface
import org.horizontal.tella.mobile.views.interfaces.IMainNavigationInterface
import org.horizontal.tella.mobile.views.interfaces.VerificationWorkStatusCallback
import org.hzontal.shared_ui.bottomsheet.BottomSheetUtils.showStandardSheet
import org.hzontal.shared_ui.utils.DialogUtils
import org.javarosa.core.model.FormIndex
import org.javarosa.form.api.FormEntryCaption
import org.javarosa.form.api.FormEntryPrompt
import permissions.dispatcher.NeedsPermission
import permissions.dispatcher.OnNeverAskAgain
import permissions.dispatcher.OnPermissionDenied
import permissions.dispatcher.OnShowRationale
import permissions.dispatcher.PermissionRequest
import timber.log.Timber
import java.io.FileNotFoundException

//@RuntimePermission
@AndroidEntryPoint
class CollectFormEntryActivity : MetadataActivity(), ICollectEntryInterface,
    IMainNavigationInterface,
    IFormParserContract.IView,
    IFormSaverContract.IView,
    VerificationWorkStatusCallback {
    private var upNavigationIcon: Drawable? = null
    private var currentScreenView: View? = null

    //private int sectionIndex;
    private var formTitle: String? = null
    private var formParser: FormParser? = null
    private var formSaver: FormSaver? = null
    private var disposables =
        MyApplication.bus().createCompositeDisposable()
    private var endView: CollectFormEndView? = null
    private var alertDialog: AlertDialog? = null
    private var progressDialog: ProgressDialog? = null
    private var deleteEnabled = false
    private var draftAutoSaved = false
    private var micFragment: MicFragment? = null
    private lateinit var binding: ActivityCollectFormEntryBinding

    private val viewModel: SubmitFormsViewModel by viewModels()
    private val attachmentModel: QuestionAttachmentModel by viewModels()

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCollectFormEntryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applyEdgeToEdge(binding.root)
        currentScreenView = null
        //sectionIndex = 0;
        setSupportActionBar(binding.toolbar)
        val actionBar = supportActionBar
        actionBar?.setDisplayHomeAsUpEnabled(true)
        upNavigationIcon = binding.toolbar.navigationIcon
        setToolbarIcon()
        initForm()
        initObservers()
        initView()
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.collect_form_entry_menu, menu)
        if (hideMenuItems(menu)) {
            return true
        }

        // turn menu items off if needed
        var menuItem: MenuItem
        if (!deleteEnabled) {
            menuItem = menu.findItem(R.id.deleteFormMenuItem)
            menuItem.isEnabled = false
            menuItem.isVisible = false
        }
        val forLater = formParser != null && (formParser!!.isFormFinal || formParser!!.isFormEnd)
        menuItem = menu.findItem(R.id.saveFormMenuItem)
        menuItem.isVisible = !forLater
        menuItem.isEnabled = !forLater
        menuItem = menu.findItem(R.id.saveForLaterMenuItem)
        menuItem.isVisible = forLater
        menuItem.isEnabled = forLater
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == R.id.saveFormMenuItem) {
            if (formSaver != null) {
                saveCurrentScreen(false)
                formSaver!!.saveActiveFormInstance()
            }
            return true
        }
        if (id == R.id.saveForLaterMenuItem) {
            viewModel.saveForLaterFormInstance(formTitle + " " + Util.getDateTimeString())
            return true
        }
        if (id == R.id.deleteFormMenuItem) {
            deleteFormInstance()
            return true
        }
        if (id == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun initForm() {
        formSaver = FormSaver(this)
        formParser = FormParser(this)
        formParser?.parseForm()
    }

    private fun initView() {
        onGpsPermissionsListener()
        onAudioRecordingListener()
        binding.appbar.outlineProvider = null
        binding.prevSection.setOnClickListener { v -> showPrevScreen() }
        binding.nextSection.setOnClickListener { v -> showNextScreen() }
        binding.submitButton.setOnClickListener { v ->
            viewModel.submitActiveFormInstance(formTitle + " " + Util.getDateTimeString())
            hideToolbarIcon()
            hideSubmitButtons()
            showFormCancelButton()
        }
        binding.cancelButton.setOnClickListener { v ->
            if (userStoppedSubmission()) {
                hideFormCancelButton()
            }
        }

        endView = CollectFormEndView(this, R.string.Uwazi_Submitted_Entity_Header_Title)
        disposables.wire(
            MediaFileBinaryWidgetCleared::class.java,
            object : EventObserver<MediaFileBinaryWidgetCleared?>() {
                override fun onNext(event: MediaFileBinaryWidgetCleared) {
                    if (formParser != null) {
                        formParser!!.removeWidgetMediaFile(event.filename)
                    }
                    clearedFormIndex(event.formIndex)
                }
            })

    }

    private fun initObservers() {
        with(viewModel) {
            showFormSubmitLoading.observe(this@CollectFormEntryActivity) { instance: CollectFormInstance ->
                showFormSubmitLoading(instance)
            }

            formPartSubmitStart.observe(this@CollectFormEntryActivity) { (first, second): Pair<CollectFormInstance, String> ->
                formPartSubmitStart(first, second)
            }

            progressCallBack.observe(this@CollectFormEntryActivity) { (first, second): Pair<String, Float> ->
                formPartUploadProgress(first, second)
            }

            formPartSubmitSuccess.observe(this@CollectFormEntryActivity) { (first, second): Pair<CollectFormInstance, OpenRosaPartResponse?> ->
                divviupUtils.runODKSentEvent()
                second?.let { formPartSubmitSuccess(first, second) }
            }

            formSubmitNoConnectivity.observe(this@CollectFormEntryActivity) { value: Boolean ->
                formSubmitNoConnectivity()
            }

            formPartSubmitError.observe(this@CollectFormEntryActivity) { throwable: Throwable? ->
                throwable?.let { formPartSubmitError(throwable) }
            }

            hideFormSubmitLoading.observe(this@CollectFormEntryActivity) {
                hideFormSubmitLoading()
            }

            formPartsSubmitEnded.observe(this@CollectFormEntryActivity) { instance: CollectFormInstance ->
                formPartsSubmitEnded(instance)
            }

            saveForLaterFormInstanceSuccess.observe(this@CollectFormEntryActivity) {
                saveForLaterFormInstanceSuccess()
            }

            saveForLaterFormInstanceError.observe(this@CollectFormEntryActivity) { throwable: Throwable? ->
                throwable?.let { saveForLaterFormInstanceError(throwable) }
            }

            submissionStoppedByUser.observe(this@CollectFormEntryActivity) {
                submissionStoppedByUser()
            }
        }

        with(attachmentModel) {

            onMediaFileAdded.observe(this@CollectFormEntryActivity) { vaultFile: VaultFile? ->
                vaultFile?.let {
                    onMediaFileAdded(vaultFile)
                }
            }

            onImportStarted.observe(this@CollectFormEntryActivity) { value: Boolean ->
                onImportStarted()
            }

            onImportEnded.observe(this@CollectFormEntryActivity) { value: Boolean ->
                onImportEnded()
            }

            onMediaFileImported.observe(this@CollectFormEntryActivity) { vaultFile: VaultFile? ->
                vaultFile?.let {
                    onMediaFileImported(vaultFile)
                }
            }

            onImportError.observe(this@CollectFormEntryActivity) { throwable: Throwable? ->
                throwable?.let {
                    onImportError(throwable)
                }
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (!isLocationSettingsRequestCode(requestCode) && resultCode != RESULT_OK) {
            formParser!!.stopWaitingBinaryData() // remove info about waiting index
            return
        }
        when (requestCode) {
            C.MEDIA_FILE_ID -> {
                val vaultFile =
                    data!!.getSerializableExtra(CameraActivity.MEDIA_FILE_KEY) as VaultFile?
                putVaultFileInForm(vaultFile)
            }

            C.SELECTED_LOCATION -> {
                val myLocation =
                    data!!.getSerializableExtra(LocationMapActivity.SELECTED_LOCATION) as MyLocation?
                if (currentScreenView is CollectFormView) {
                    val cfv = currentScreenView as CollectFormView
                    if (myLocation != null) {
                        cfv.setBinaryData(myLocation)
                    } else {
                        cfv.clearBinaryData()
                    }
                }
                formParser?.stopWaitingBinaryData()
                saveCurrentScreen(false)
            }

            C.IMPORT_IMAGE -> {
                val image = data?.data
                if (image != null) {
                    attachmentModel.importImage(image)
                }
            }

            C.IMPORT_VIDEO -> {
                val video = data?.data
                if (video != null) {
                    attachmentModel.importVideo(video)
                }
            }

            C.IMPORT_FILE -> {
                val video = data?.data
                if (video != null) {
                    attachmentModel.importFile(video)
                }
            }
        }
    }

    private fun putVaultFileInForm(vaultFile: VaultFile?) {
        if (currentScreenView is CollectFormView) {
            val cfv = currentScreenView as CollectFormView
            if (vaultFile != null) {
                val filename = cfv.setBinaryData(vaultFile)
                if (filename != null) {
                    formParser?.setWidgetMediaFile(filename, vaultFile)
                    formParser?.setTellaMetadataFields(cfv, vaultFile.metadata)
                } else {
                    Timber.e("Binary data not set on waiting widget")
                }
            } else {
                formParser?.removeWidgetMediaFile(cfv.clearBinaryData())
                formParser?.clearTellaMetadataFields(cfv)
            }
        }
        formParser?.stopWaitingBinaryData()
        saveCurrentScreen(false)
    }

    /*   @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        CollectFormEntryActivityPermissionsDispatcher.onRequestPermissionsResult(this, requestCode, grantResults);
    }*/
    @NeedsPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    fun startPermissionProcess() {
        manageLocationSettings(
            C.GPS_PROVIDER
        ) {}
    }

    @OnShowRationale(Manifest.permission.ACCESS_FINE_LOCATION)
    fun showFineLocationRationale(request: PermissionRequest?) {
        maybeChangeTemporaryTimeout()
        alertDialog = showRationale(this, request!!, getString(R.string.permission_dialog_expl_GPS))
    }

    @OnPermissionDenied(Manifest.permission.ACCESS_FINE_LOCATION)
    fun onFineLocationPermissionDenied() {
    }

    @OnNeverAskAgain(Manifest.permission.ACCESS_FINE_LOCATION)
    fun onFineLocationNeverAskAgain() {
    }

    private fun hideMenuItems(menu: Menu): Boolean {
        val submitting = isSubmitting
        for (i in 0 until menu.size()) {
            menu.getItem(i).isEnabled = !submitting
            menu.getItem(i).isVisible = !submitting
        }
        return submitting
    }

    private fun setToolbarIcon() {
        binding.toolbar.isEnabled = true
        if (formParser != null && formParser!!.isFormEnd && !formParser!!.isFormFinal) {
            binding.toolbar.navigationIcon = upNavigationIcon
        } else {
            binding.toolbar.setNavigationIcon(R.drawable.ic_close_white_24dp)
        }
    }

    private fun hideToolbarIcon() {
        binding.toolbar.isEnabled = false
    }

    private fun clearedFormIndex(formIndex: FormIndex) {
        if (formParser != null) {
            if (currentScreenView is CollectFormView) {
                val cfv = currentScreenView as CollectFormView
                formParser!!.clearTellaMetadataFields(formIndex, cfv)
            }
        }
    }

    private fun isLocationSettingsRequestCode(requestCode: Int): Boolean {
        return requestCode == C.GPS_PROVIDER
    }

    private fun deleteFormInstance() {
        if (formSaver == null) return
        val cloned = formSaver!!.isActiveInstanceCloned
        alertDialog = DialogsUtil.showFormInstanceDeleteDialog(
            this,
            if (cloned) CollectFormInstanceStatus.SUBMITTED else CollectFormInstanceStatus.UNKNOWN
        ) { dialog: DialogInterface?, which: Int ->
            if (formSaver != null) {
                formSaver!!.deleteActiveFormInstance()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        startLocationMetadataListening()
    }

    override fun onPause() {
        super.onPause()
        closeAlertDialog()
        if (isSubmitting) {
            viewModel.stopSubmission()
            refreshFormEndView(false)
            hideFormCancelButton()
            showFormEndButtons()
            invalidateOptionsMenu()
        }
        saveCurrentScreen(false)
    }

    override fun onStop() {
        stopLocationMetadataListening()
        super.onStop()
    }

    override fun onDestroy() {
        disposables.dispose()
        destroyFormParser()
        destroyFormSaver()
        super.onDestroy()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        super.onBackPressed()
        if (isSubmitting) {
            alertDialog = DialogsUtil.showExitWithSubmitDialog(this,
                { dialog: DialogInterface?, which: Int ->
                    stopSubmission()
                    onBackPressedNotSubmitting(true)
                }
            ) { dialog: DialogInterface?, which: Int -> }
        } else {
            onBackPressedNotSubmitting(false)
        }
    }

    private fun onBackPressedNotSubmitting(stopped: Boolean) {
        if (formParser == null) {
            onBackPressedWithoutCheck()
            return
        }
        if (formParser!!.isFormEnd && !formParser!!.isFormFinal) {
            hideFormCancelButton()
            hideSubmitButtons()
            showPrevScreen()
            invalidateOptionsMenu()
        } else if (formParser!!.isFormChanged && !formParser!!.isFormFinal) {
            showFormChangedDialog()
        } else if (formParser!!.isFormFinal && !stopped) {
            showStandardSheet(
                this.supportFragmentManager,
                getString(R.string.Collect_DialogTitle_StopExit),
                getString(R.string.Collect_DialogExpl_ExitingStopSubmission),
                getString(R.string.Collect_DialogAction_KeepSubmitting),
                getString(R.string.Collect_DialogAction_StopAndExit),
                null
            ) { onDialogBackPressed() }
        } else {
            onBackPressedWithoutCheck()
        }
    }

    private fun onDialogBackPressed() {
        onBackPressedWithoutCheck()
        return
    }

    private fun onBackPressedWithoutCheck() {
        super.onBackPressed()
    }

    override fun formBeginning(title: String) {
        formTitle = title
        setTitle(formTitle)
        formParser!!.stepToNextScreen()
    }

    override fun formEnd(instance: CollectFormInstance) {
        Util.hideKeyboard(this, endView)
        showFormEndView(instance)
        hideSectionButtons()
        setToolbarIcon()
        invalidateOptionsMenu()
    }

    override fun formQuestion(prompts: Array<FormEntryPrompt>, groups: Array<FormEntryCaption>) {
        showFormView(CollectFormView(this, prompts, groups))
        setSectionButtons()
        setToolbarIcon()
    }

    override fun formGroup(prompts: Array<FormEntryPrompt>, groups: Array<FormEntryCaption>) {
        showFormView(CollectFormView(this, prompts, groups))
        setSectionButtons()
        setToolbarIcon()
    }

    override fun formRepeat(prompts: Array<FormEntryPrompt>, groups: Array<FormEntryCaption>) {
        showFormView(CollectFormView(this, prompts, groups))
        setSectionButtons()
        setToolbarIcon()
    }

    override fun formPromptNewRepeat(lastRepeatCount: Int, groupText: String) {
        createPromptDialog(lastRepeatCount, groupText)
    }

    override fun formParseError(error: Throwable) {
        DialogUtils.showBottomMessage(
            this,
            getString(R.string.collect_toast_fail_parsing_form),
            true
        )
    }

    override fun formSaveError(error: Throwable) {
        Timber.d(error, javaClass.name)
    }

    override fun showSaveFormInstanceLoading() {}
    override fun hideSaveFormInstanceLoading() {}
    override fun showDeleteFormInstanceStart() {}
    override fun hideDeleteFormInstanceEnd() {}
    override fun formInstanceSaveError(throwable: Throwable) {}
    override fun formInstanceSaveSuccess(instance: CollectFormInstance) {
        DialogUtils.showBottomMessage(
            this,
            getFormSaveMsg(instance),
            false
        )
        formParser!!.startFormChangeTracking()
        SharedLiveData.updateViewPagerPosition.postValue(DRAFT_LIST_PAGE_INDEX)
    }

    override fun formInstanceAutoSaveSuccess(instance: CollectFormInstance) {
        DialogUtils.showBottomMessage(
            this,
            getFormSaveMsg(instance),
            false
        )
        formParser!!.startFormChangeTracking()
        draftAutoSaved = true
    }

    private fun getFormSaveMsg(instance: CollectFormInstance): String {
        return when (instance.status) {
            CollectFormInstanceStatus.UNKNOWN, CollectFormInstanceStatus.DRAFT -> getString(R.string.collect_toast_draft_saved)
            else -> getString(R.string.collect_toast_draft_saved)
        }
    }

    override fun formInstanceDeleteSuccess(cloned: Boolean) {
        finish()
    }

    override fun formInstanceDeleteError(throwable: Throwable) {
        Timber.d(throwable, javaClass.name)
    }

    private fun showFormSubmitLoading(instance: CollectFormInstance) {
        invalidateOptionsMenu()
        endView!!.clearPartsProgress(instance)
        disableScreenTimeout()
    }

    private fun hideFormSubmitLoading() {
        setToolbarIcon()
        invalidateOptionsMenu()
        enableScreenTimeout()
    }

    private fun formSubmitError(error: Throwable) {
        val errorMessage = FormUtils.getFormSubmitErrorMessage(this, error)
        DialogUtils.showBottomMessage(
            this,
            errorMessage,
            true
        )
        SharedLiveData.updateViewPagerPosition.postValue(OUTBOX_LIST_PAGE_INDEX)
        finish()
    }

    private fun formSubmitNoConnectivity() {
        DialogUtils.showBottomMessage(
            this,
            getString(R.string.collect_end_toast_notification_form_not_sent_no_connection),
            true
        )
        SharedLiveData.updateViewPagerPosition.postValue(SUBMITTED_LIST_PAGE_INDEX)
        finish()
    }

    private fun formPartSubmitStart(instance: CollectFormInstance, partName: String) {
        endView!!.showUploadProgress(partName)
        invalidateOptionsMenu()
        binding.toolbar.setNavigationIcon(R.drawable.ic_close_white_24dp)
    }

    private fun formPartUploadProgress(partName: String, pct: Float) {
        endView!!.setUploadProgress(partName, pct)
    }

    private fun formPartSubmitSuccess(
        instance: CollectFormInstance,
        response: OpenRosaPartResponse
    ) {
        endView!!.hideUploadProgress(response.partName)
    }

    private fun formPartSubmitError(error: Throwable) {
        // error on part stops entire submission
        formSubmitError(error)
    }

    private fun formPartsSubmitEnded(instance: CollectFormInstance) {
        DialogUtils.showBottomMessage(
            this,
            getString(R.string.collect_toast_form_submitted),
            false
        )
        SharedLiveData.updateViewPagerPosition.postValue(SUBMITTED_LIST_PAGE_INDEX)
        finish()
    }

    private fun submissionStoppedByUser() {
        refreshFormEndView(false)
        hideFormCancelButton()
        showFormEndButtons()
        SharedLiveData.updateViewPagerPosition.postValue(OUTBOX_LIST_PAGE_INDEX)
    }

    override fun formConstraintViolation(formIndex: FormIndex, errorString: String) {
        if (currentScreenView is CollectFormView) {
            (currentScreenView as CollectFormView).setValidationConstraintText(
                formIndex,
                errorString
            )
            DialogUtils.showBottomMessage(
                this,
                getString(R.string.collect_form_toast_validation_generic_error),
                true
            )
        }
    }

    fun saveForLaterFormInstanceSuccess() {
        DialogUtils.showBottomMessage(
            this,
            getString(R.string.collect_toast_form_saved_for_later_submission),
            false
        )
        SharedLiveData.updateViewPagerPosition.postValue(OUTBOX_LIST_PAGE_INDEX)
        finish()
    }

    private fun saveForLaterFormInstanceError(throwable: Throwable) {
        Timber.d(throwable, javaClass.name)
    }

    override fun formPropertiesChecked(enableDelete: Boolean) {
        var invalidateOptionsMenu = false
        if (enableDelete) {
            deleteEnabled = true
            invalidateOptionsMenu = true
        }
        if (invalidateOptionsMenu) {
            invalidateOptionsMenu()
        }
    }

    private fun onMediaFileAdded(vaultFile: VaultFile) {
        onActivityResult(
            C.MEDIA_FILE_ID,
            RESULT_OK,
            Intent().putExtra(QuestionAttachmentActivity.MEDIA_FILE_KEY, vaultFile)
        )
    }

    private fun onMediaFileImported(vaultFile: VaultFile) {
        onMediaFileAdded(vaultFile)
    }

    private fun onImportError(error: Throwable) {
        val messageResId = when (error) {
            is FileNotFoundException -> R.string.error_file_not_found
            else -> R.string.gallery_toast_fail_importing_file
        }
        DialogUtils.showBottomMessage(
            this,
            getString(messageResId),
            true
        )
        Timber.d(error, javaClass.name)
    }

    private fun onImportStarted() {
        progressDialog =
            DialogsUtil.showProgressDialog(this, getString(R.string.gallery_dialog_expl_encrypting))
    }

    private fun onImportEnded() {
        hideProgressDialog()
        DialogUtils.showBottomMessage(
            this,
            getString(R.string.gallery_toast_file_encrypted),
            false
        )
    }

    override fun formSavedOnExit() {
        SharedLiveData.updateViewPagerPosition.postValue(OUTBOX_LIST_PAGE_INDEX)
        closeAlertDialog()
        onBackPressedWithoutCheck()
    }

    override fun getContext(): Context {
        return this
    }

    private fun closeAlertDialog() {
        if (alertDialog != null && alertDialog!!.isShowing) {
            alertDialog!!.dismiss()
        }
    }

    private fun createPromptDialog(lastRepeatCount: Int, groupText: String) {
        if (alertDialog != null && alertDialog!!.isShowing) {
            return
        }
        alertDialog = AlertDialog.Builder(this, R.style.PurpleBackgroundLightLettersDialogTheme)
            .setPositiveButton(R.string.collect_form_dialog_action_add_group) { dialog, which -> formParser!!.executeRepeat() }
            .setNegativeButton(R.string.collect_form_dialog_action_dont_add_group) { dialog, which -> formParser!!.cancelRepeat() }
            .setCancelable(false)
            .create()
        if (lastRepeatCount > 0) {
            alertDialog!!.setTitle(
                getString(
                    R.string.collect_form_dialog_title_add_additional_group,
                    groupText
                )
            )
            alertDialog!!.setMessage(
                getString(
                    R.string.collect_form_dialog_expl_add_additional_group,
                    groupText
                )
            )
        } else {
            alertDialog!!.setTitle(
                getString(
                    R.string.collect_form_dialog_action_add_first_group,
                    groupText
                )
            )
            alertDialog!!.setMessage(
                getString(
                    R.string.collect_form_dialog_expl_add_first_group,
                    groupText
                )
            )
        }
        alertDialog!!.show()
    }

    private fun formAttachmentsChanged() {
        invalidateOptionsMenu()
    }

    private fun showScreenView(view: View?) {
        if (currentScreenView != null) {
            binding.screenFormView.removeView(currentScreenView)
        }
        currentScreenView = view
        binding.screenFormView.addView(currentScreenView!!)
    }

    private fun showFormView(view: CollectFormView) {
        hideKeyboard()
        showScreenView(view)
        view.setFocus(this)
    }

    private fun showFormEndView(instance: CollectFormInstance) {
        hideKeyboard()
        showFormEndButtons()
        endView!!.setInstance(instance, false)
        showScreenView(endView)
    }

    private fun refreshFormEndView(@Suppress("SameParameterValue") offline: Boolean) {
        endView!!.refreshInstance(offline)
    }

    private fun showNextScreen() {
        if (saveCurrentScreen(true)) {
            formSaver!!.autoSaveFormInstance()
            formParser!!.stepToNextScreen()
        }
    }

    private fun showPrevScreen() {
        if (saveCurrentScreen(false)) {
            formSaver!!.autoSaveFormInstance()
            formParser!!.stepToPrevScreen()
        }
    }

    private fun saveCurrentScreen(checkConstraints: Boolean): Boolean {
        if (currentScreenView is CollectFormView) {
            val cfv = currentScreenView as CollectFormView
            if (checkConstraints) {
                cfv.clearValidationConstraints()
            }
            return formSaver!!.saveScreenAnswers(cfv.answers, checkConstraints)
        }
        return true
    }

    private fun hideKeyboard() {
        val v = currentFocus
        if (v != null) {
            Util.hideKeyboard(this, v)
        }
    }

    private fun destroyFormSaver() {
        if (formSaver != null) {
            formSaver!!.destroy()
            formSaver = null
        }
    }

    private fun destroyFormParser() {
        if (formParser != null) {
            formParser!!.destroy()
            formParser = null
        }
    }

    private fun setFirstSectionButtons() {
        hideFormCancelButton()
        hideSubmitButtons()
        hidePrevSectionButton()
    }

    private fun setSectionButtons() {
        binding.buttonBottomLayout.visibility = View.VISIBLE
        showNextSectionButton()
        if (formParser!!.isFirstScreen) {
            setFirstSectionButtons()
            return
        }
        binding.prevSection.isEnabled = true
        binding.prevSection.visibility = View.VISIBLE
    }

    private fun showFormEndButtons() {
        //setSubmitButtonText(Preferences.isOfflineMode())
        binding.submitButton.isEnabled = true
        binding.submitButton.visibility = View.VISIBLE
    }

    private fun hidePrevSectionButton() {
        binding.prevSection.isEnabled = false
        binding.prevSection.visibility = View.GONE
    }

    private fun hideSectionButtons() {
        hidePrevSectionButton()
        binding.nextSection.isEnabled = false
        binding.nextSection.visibility = View.GONE
    }

    private fun hideSubmitButtons() {
        binding.submitButton.isEnabled = false
        binding.submitButton.visibility = View.GONE
    }

    private fun showNextSectionButton() {
        binding.nextSection.isEnabled = true
        binding.nextSection.visibility = View.VISIBLE
    }

    private fun showFormCancelButton() {
        binding.cancelButton.isEnabled = true
        binding.cancelButton.visibility = View.VISIBLE
    }

    private fun hideFormCancelButton() {
        binding.cancelButton.isEnabled = false
        binding.cancelButton.visibility = View.GONE
    }

    private fun showFormChangedDialog() {
        showStandardSheet(
            this.supportFragmentManager,
            getString(R.string.collect_form_exit_unsaved_dialog_title),
            getString(R.string.collect_form_exit_dialog_expl),
            getString(R.string.collect_form_exit_dialog_action_save_exit),
            getString(R.string.collect_form_exit_dialog_action_exit_anyway),
            { onSavePressed() }) { onExitPressed() }
    }

    private fun onExitPressed() {
        onBackPressedWithoutCheck()
        return
    }

    private fun onSavePressed() {
        if (formSaver != null) {
            formSaver!!.saveActiveFormInstanceOnExit()
            onBackPressedWithoutCheck()
        } else {
            onBackPressedWithoutCheck()
        }
        return
    }

    private val isSubmitting: Boolean
        get() = viewModel.isSubmitting()

    private fun stopSubmission() {
        viewModel.stopSubmission()
        SharedLiveData.updateViewPagerPosition.postValue(OUTBOX_LIST_PAGE_INDEX)
    }

    private fun userStoppedSubmission(): Boolean {
        viewModel.userStopSubmission()
        return true
    }

    private fun hideProgressDialog() {
        if (progressDialog != null) {
            progressDialog!!.dismiss()
            progressDialog = null
        }
    }

    private fun disableScreenTimeout() {
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun enableScreenTimeout() {
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    override fun hideBottomNavigation() {
        binding.btmNavMain.hide()
    }

    override fun showBottomNavigation() {
        binding.btmNavMain.show()
    }

    override fun openAudioRecorder() {
        val intent = Intent(this, MicActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
        intent.putExtra(VAULT_CURRENT_ROOT_PARENT, "")
        intent.putExtra(COLLECT_ENTRY, true)
        startActivity(intent)
    }

    override fun returnFileToForm(file: VaultFile) {
        binding.entryLayout.visibility = View.VISIBLE
        putVaultFileInForm(file)
        if (micFragment != null) {
            supportFragmentManager.beginTransaction().remove((micFragment as Fragment?)!!).commit()
        }
    }

    override fun stopWaitingForData() {
        binding.entryLayout.visibility = View.VISIBLE
        formParser!!.stopWaitingBinaryData()
        saveCurrentScreen(false)
    }

    private fun hasGpsPermissions(context: Context): Boolean {
        return ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestGpsPermissions(requestCode: Int) {
        requestPermissions(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION
            ), requestCode
        )
    }

    private fun onGpsPermissionsListener() {
        disposables.wire(
            LocationPermissionRequiredEvent::class.java,
            object : EventObserver<LocationPermissionRequiredEvent?>() {
                override fun onNext(event: LocationPermissionRequiredEvent) {
                    if (!hasGpsPermissions(context)) {
                        maybeChangeTemporaryTimeout {
                            requestGpsPermissions(C.GPS_PROVIDER)
                        }
                    }
                }
            })
    }

    private fun onAudioRecordingListener() {
        disposables.wire(
            AudioRecordEvent::class.java,
            object : EventObserver<AudioRecordEvent?>() {
                override fun onNext(event: AudioRecordEvent) {
                    putVaultFileInForm(event.vaultFile)
                }
            })
    }

    override fun isBackgroundWorkInProgress(): Boolean {
        return false
    }

    override fun showBackgroundWorkAlert() {
        //TODO WAFA HANDLE BACKGROUND PROCESS IN COLLECT
    }

    override fun setBackgroundWorkStatus(inProgress: Boolean) {

    }
}