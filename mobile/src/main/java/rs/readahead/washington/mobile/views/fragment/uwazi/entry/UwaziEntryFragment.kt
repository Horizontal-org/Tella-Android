package rs.readahead.washington.mobile.views.fragment.uwazi.entry

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.NavHostFragment
import com.hzontal.tella_vault.MyLocation
import org.hzontal.shared_ui.bottomsheet.BottomSheetUtils
import org.hzontal.shared_ui.utils.DialogUtils
import rs.readahead.washington.mobile.MyApplication
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.bus.EventObserver
import rs.readahead.washington.mobile.bus.event.LocationPermissionRequiredEvent
import rs.readahead.washington.mobile.databinding.UwaziEntryFragmentBinding
import rs.readahead.washington.mobile.domain.entity.uwazi.UwaziEntityStatus
import rs.readahead.washington.mobile.presentation.uwazi.UwaziGeoData
import rs.readahead.washington.mobile.util.C
import rs.readahead.washington.mobile.views.activity.LocationMapActivity
import rs.readahead.washington.mobile.views.base_ui.BaseBindingFragment
import rs.readahead.washington.mobile.views.fragment.uwazi.SharedLiveData
import rs.readahead.washington.mobile.views.fragment.uwazi.attachments.VAULT_FILE_KEY
import rs.readahead.washington.mobile.views.fragment.uwazi.send.SEND_ENTITY
import rs.readahead.washington.mobile.views.fragment.uwazi.viewpager.DRAFT_LIST_PAGE_INDEX
import rs.readahead.washington.mobile.views.fragment.uwazi.viewpager.OUTBOX_LIST_PAGE_INDEX
import rs.readahead.washington.mobile.views.fragment.uwazi.viewpager.SUBMITTED_LIST_PAGE_INDEX
import rs.readahead.washington.mobile.views.fragment.vault.attachements.OnNavBckListener


const val COLLECT_TEMPLATE = "collect_template"
const val UWAZI_INSTANCE = "uwazi_instance"
const val UWAZI_TITLE = "title"
const val UWAZI_SUPPORTING_FILES = "supporting_files"
const val UWAZI_PRIMARY_DOCUMENTS = "primary_documents"

class UwaziEntryFragment :
    BaseBindingFragment<UwaziEntryFragmentBinding>(UwaziEntryFragmentBinding::inflate),
    OnNavBckListener {

    private val viewModel: SharedUwaziSubmissionViewModel by lazy {
        ViewModelProvider(baseActivity).get(SharedUwaziSubmissionViewModel::class.java)
    }
    private val uwaziParser: UwaziParser by lazy { UwaziParser(context) }

    private val bundle by lazy { Bundle() }
    private var screenView: ViewGroup? = null
    private lateinit var uwaziFormView: UwaziFormView

    private val disposables by lazy { MyApplication.bus().createCompositeDisposable() }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initObservers()
        if (!hasInitializedRootView) {
            hasInitializedRootView = true
            initView()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == C.MEDIA_FILE_ID && resultCode == Activity.RESULT_OK) {
            val vaultFile = data?.getStringExtra(VAULT_FILE_KEY) ?: ""
            putVaultFileInForm(vaultFile)
        }

        if (requestCode == C.SELECTED_LOCATION && resultCode == Activity.RESULT_OK) {
            val myLocation: MyLocation =
                data!!.getSerializableExtra(LocationMapActivity.SELECTED_LOCATION) as MyLocation
            putLocationInForm(UwaziGeoData("", myLocation.latitude, myLocation.longitude))
        }
    }

    private fun initView() {
        binding?.apply {
            toolbar.backClickListener = { onBackPressed() }
            toolbar.onRightClickListener = {
                uwaziParser.setInstanceStatus(UwaziEntityStatus.DRAFT)
                if (!uwaziParser.getAnswersFromForm(false, uwaziFormView)) {
                    uwaziFormView.setFocus(context)
                    showValidationMandatoryTitleDialog()
                } else {
                    uwaziParser.getInstance().let { viewModel.saveEntityInstance(it) }
                }
            }

            nextBtn.setOnClickListener { sendEntity() }
            screenView = screenFormView
        }

        onGpsPermissionsListener()

        if (arguments?.getString(UWAZI_INSTANCE) != null) {
            arguments?.getString(UWAZI_INSTANCE).let {
                if (it != null) {
                    parseUwaziInstance(it)
                }
            }

        }

        if (arguments?.getString(COLLECT_TEMPLATE) != null) {
            arguments?.getString(COLLECT_TEMPLATE).let {
                if (it != null) {
                    parseUwaziTemplate(it)
                }
            }
        }
        binding!!.toolbar.setStartTextTitle(uwaziParser.getTemplate()?.entityRow?.translatedName.toString())
    }


    private fun initObservers() {
        with(viewModel) {
            progress.observe(viewLifecycleOwner, { status ->
                when (status) {
                    UwaziEntityStatus.SUBMISSION_PENDING, UwaziEntityStatus.SUBMISSION_ERROR -> {
                        SharedLiveData.updateViewPagerPosition.postValue(OUTBOX_LIST_PAGE_INDEX)
                        nav().popBackStack()
                        progress.postValue(UwaziEntityStatus.UNKNOWN)
                    }
                    UwaziEntityStatus.SUBMITTED -> {
                        SharedLiveData.updateViewPagerPosition.postValue(SUBMITTED_LIST_PAGE_INDEX)
                        nav().popBackStack()
                        progress.postValue(UwaziEntityStatus.UNKNOWN)
                    }
                    UwaziEntityStatus.DRAFT -> {
                        SharedLiveData.updateViewPagerPosition.postValue(DRAFT_LIST_PAGE_INDEX)
                        nav().popBackStack()
                        showSavedDialog()
                        progress.postValue(UwaziEntityStatus.UNKNOWN)
                    }
                    else -> {}
                }
            })
        }
    }

    private fun sendEntity() {
        if (!uwaziParser.getAnswersFromForm(true, uwaziFormView)) {
            uwaziFormView.setFocus(context)
            //showValidationMandatoryFieldsDialog()
            showValidationErrorsFieldsDialog()
        } else {
            bundle.putString(SEND_ENTITY, uwaziParser.getGsonTemplate())
            NavHostFragment.findNavController(this@UwaziEntryFragment)
                .navigate(R.id.action_uwaziEntryScreen_to_uwaziSendScreen, bundle)
        }
    }

    private fun parseUwaziInstance(instance: String) {
        uwaziFormView = uwaziParser.parseInstance(instance)
        screenView?.addView(uwaziFormView)
        uwaziParser.putAnswersToForm(uwaziFormView)
    }

    private fun parseUwaziTemplate(template: String) {
        uwaziFormView = uwaziParser.parseTemplate(template)
        screenView?.addView(uwaziFormView)
        uwaziParser.fillAnswersToForm(uwaziFormView)
    }

    private fun putVaultFileInForm(vaultFile: String) {
        vaultFile.let { uwaziFormView.setBinaryData(it) }
    }

    private fun putLocationInForm(location: UwaziGeoData) {
        uwaziFormView.setBinaryData(location)
    }

    private fun showSavedDialog() {
        DialogUtils.showBottomMessage(
            baseActivity,
            getString(R.string.Uwazi_EntryInstance_SavedInfo),
            false
        )
    }

    private fun showValidationMandatoryFieldsDialog() {
        DialogUtils.showBottomMessage(
            baseActivity,
            getString(R.string.Uwazi_Entry_Validation_MandatoryFields),
            false
        )
    }

    private fun showValidationErrorsFieldsDialog() {
        DialogUtils.showBottomMessage(
            baseActivity,
            getString(R.string.collect_form_toast_validation_generic_error),
            false
        )
    }

    private fun showValidationMandatoryTitleDialog() {
        DialogUtils.showBottomMessage(
            baseActivity,
            getString(R.string.Uwazi_Entry_Validation_MandatoryTitle),
            false
        )
    }

    private fun hasGpsPermissions(context: Context): Boolean {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
            return true
        return false
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
                    if (!hasGpsPermissions(requireContext())) {
                        baseActivity.maybeChangeTemporaryTimeout {
                            requestGpsPermissions(C.GPS_PROVIDER)
                        }
                    }
                }
            })
    }

    override fun onBackPressed(): Boolean {
        // The save draft dialog should be shown if the form could be saved and if the answers have changed
        if (uwaziParser.getAnswersFromForm(false, uwaziFormView)
            && uwaziParser.hashCode != uwaziFormView.answers.hashCode()
        ) {
            BottomSheetUtils.showStandardSheet(
                baseActivity.supportFragmentManager,
                baseActivity.getString(R.string.Uwazi_Dialog_Draft_Title),
                baseActivity.getString(R.string.collect_form_exit_dialog_expl),
                baseActivity.getString(R.string.collect_form_exit_dialog_action_save_exit),
                baseActivity.getString(R.string.collect_form_exit_dialog_action_exit_anyway),
                onConfirmClick = {
                    uwaziParser.setInstanceStatus(UwaziEntityStatus.DRAFT)
                    uwaziParser.getInstance().let { viewModel.saveEntityInstance(it) }
                },
                onCancelClick = {
                    nav().popBackStack()
                }
            )
        } else {
            nav().popBackStack()
        }
        return true
    }
}