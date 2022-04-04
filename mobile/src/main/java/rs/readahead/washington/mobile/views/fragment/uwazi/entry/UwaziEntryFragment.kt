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
import com.google.gson.Gson
import com.google.gson.internal.LinkedTreeMap
import com.hzontal.tella_vault.MyLocation
import com.hzontal.tella_vault.VaultFile
import org.hzontal.shared_ui.utils.DialogUtils
import rs.readahead.washington.mobile.MyApplication
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.bus.EventObserver
import rs.readahead.washington.mobile.bus.event.LocationPermissionRequiredEvent
import rs.readahead.washington.mobile.bus.event.MediaFileDeletedEvent
import rs.readahead.washington.mobile.data.uwazi.UwaziConstants
import rs.readahead.washington.mobile.databinding.UwaziEntryFragmentBinding
import rs.readahead.washington.mobile.domain.entity.collect.FormMediaFile
import rs.readahead.washington.mobile.domain.entity.uwazi.CollectTemplate
import rs.readahead.washington.mobile.domain.entity.uwazi.UwaziEntityInstance
import rs.readahead.washington.mobile.domain.entity.uwazi.UwaziEntityStatus
import rs.readahead.washington.mobile.presentation.uwazi.UwaziGeoData
import rs.readahead.washington.mobile.presentation.uwazi.UwaziValue
import rs.readahead.washington.mobile.presentation.uwazi.UwaziValueAttachment
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
        ViewModelProvider(activity).get(SharedUwaziSubmissionViewModel::class.java)
    }

    private var template: CollectTemplate? = null
    private var entityInstance: UwaziEntityInstance = UwaziEntityInstance()
    private val bundle by lazy { Bundle() }
    private var screenView: ViewGroup? = null
    private var entryPrompts = mutableListOf<UwaziEntryPrompt>()
    private lateinit var uwaziFormView: UwaziFormView

    private val uwaziTitlePrompt by lazy {
        UwaziEntryPrompt(
            UWAZI_TITLE,
            "10242048",
            UwaziConstants.UWAZI_DATATYPE_TEXT,
            "Title",
            true,
            "Enter the submission title"
        )
    }

    private val uwaziFilesPrompt by lazy {
        UwaziEntryPrompt(
            UWAZI_SUPPORTING_FILES,
            "10242049",
            UwaziConstants.UWAZI_DATATYPE_MULTIFILES,
            getString(R.string.Uwazi_MiltiFileWidget_SupportingFiles),
            false,
            getString(R.string.Uwazi_MiltiFileWidget_Help)
        )
    }

    private val disposables by lazy { MyApplication.bus().createCompositeDisposable() }

    private val uwaziPdfsPrompt by lazy {
        UwaziEntryPrompt(
            UWAZI_PRIMARY_DOCUMENTS,
            "10242050",
            UwaziConstants.UWAZI_DATATYPE_MULTIPDFFILES,
            getString(R.string.Uwazi_MiltiFileWidget_PrimaryDocuments),
            false,
            getString(R.string.Uwazi_MiltiFileWidget_AttachMenyPdfFiles)
        )
    }


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
        with(binding) {
            this!!.toolbar.backClickListener = { nav().popBackStack() }
            toolbar.onRightClickListener = {
                entityInstance.status = UwaziEntityStatus.DRAFT
                if (!getAnswersFromForm(false)) {
                    uwaziFormView.setFocus(context)
                    showValidationMandatoryTitleDialog()
                } else {
                    entityInstance.let { viewModel.saveEntityInstance(it) }
                }
            }

            nextBtn.setOnClickListener { sendEntity() }

            screenView = screenFormView
        }

        onGpsPermissionsListener()

        if (arguments?.getString(UWAZI_INSTANCE) != null) {
            arguments?.getString(UWAZI_INSTANCE).let {
                entityInstance = Gson().fromJson(it, UwaziEntityInstance::class.java)
                template = entityInstance.collectTemplate
                parseUwaziInstance()
            }

        }

        if (arguments?.getString(COLLECT_TEMPLATE) != null) {
            arguments?.getString(COLLECT_TEMPLATE).let {
                template = Gson().fromJson(it, CollectTemplate::class.java)
                entityInstance.collectTemplate = template
                entityInstance.template = template?.entityRow?.name.toString()
                parseUwaziTemplate()
            }
        }
        binding!!.toolbar.setStartTextTitle(template?.entityRow?.translatedName.toString())
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
                }
            })
        }
    }

    override fun onBackPressed(): Boolean {
        return nav().popBackStack()
    }

    private fun sendEntity() {
        //TODO REFACTOR THIS INTO A SEPARATE PARSER
        if (!getAnswersFromForm(true)) {
            uwaziFormView.setFocus(context)
            //showValidationMandatoryFieldsDialog()
            showValidationErrorsFieldsDialog()
        } else {
            val gsonTemplate = Gson().toJson(entityInstance)
            bundle.putString(SEND_ENTITY, gsonTemplate)
            NavHostFragment.findNavController(this@UwaziEntryFragment)
                .navigate(R.id.action_uwaziEntryScreen_to_uwaziSendScreen, bundle)
        }
    }

    private fun getAnswersFromForm(isSend: Boolean): Boolean {
        //TODO REFACTOR THIS INTO A SEPARATE PARSER
        uwaziFormView.clearValidationConstraints()
        val hashmap = mutableMapOf<String, List<Any>>()
        val widgetMediaFiles = mutableListOf<FormMediaFile>()
        val answers = uwaziFormView.answers
        var validationRequired = false
        var validationError = false

        // check required fields
        if (answers[UWAZI_TITLE] == null) {
            uwaziFormView.setValidationConstraintText(
                UWAZI_TITLE,
                getString(R.string.Uwazi_Entity_Error_Response_Mandatory)
            )
            validationRequired = true
        }

        if (isSend) {
            for (property in template?.entityRow?.properties!!) {
                //check url validation errors
                if (uwaziFormView.checkValidationConstraints()) {
                    validationError = true
                }

                //check mandatory errors
                if (property.required && (answers[property.name] == null)) {
                    uwaziFormView.setValidationConstraintText(
                        property.name,
                        getString(R.string.Uwazi_Entity_Error_Response_Mandatory)
                    )
                    validationRequired = true
                }
            }
        }
        if (validationRequired || validationError) return false

        // put answers to entity
        for (answer in answers) {
            if (answer.value != null) {
                if (answer.key == UWAZI_TITLE) {
                    entityInstance.title = (answer.value as UwaziValue).value as String
                } else {
                    when (answer.value) {
                        is List<*> -> {
                            hashmap[answer.key] = (answer.value) as List<UwaziValue>
                        }
                        is UwaziValueAttachment -> {
                            hashmap[answer.key] = arrayListOf(
                                UwaziValueAttachment(
                                    value = (answer.value as UwaziValueAttachment).value,
                                    attachment = uwaziFormView.filesNames.indexOf((answer.value as UwaziValueAttachment).value)
                                )
                            )
                        }
                        else -> {
                            hashmap[answer.key] =
                                arrayListOf(UwaziValue((answer.value as UwaziValue).value))
                        }
                    }
                }
            }
        }

        //put files in entity
        for (answer in uwaziFormView.files) {
            if (answer != null) {
                widgetMediaFiles.add(answer)
            }
        }
        entityInstance.metadata = hashmap
        entityInstance.widgetMediaFiles = widgetMediaFiles
        entityInstance.collectTemplate = template
        entityInstance.template = template?.entityRow?.name.toString()
        return true
    }

    private fun putAnswersToForm(instance: UwaziEntityInstance, formView: UwaziFormView) {
        //TODO REFACTOR THIS INTO A SEPARATE PARSER
        val files = mutableMapOf<String, FormMediaFile>()
        for (file in instance.widgetMediaFiles) {
            files[file.name] = file
        }

        formView.setBinaryData(UWAZI_TITLE, instance.title)
        for (answer in instance.metadata) {

            val stringVal = if ((instance.metadata[answer.key] as ArrayList).size == 1) {
                (instance.metadata[answer.key]?.get(0) as LinkedTreeMap<String, Any>)["value"]
            } else {
                (instance.metadata[answer.key])
            }

            if (files.containsKey(stringVal)) {
                formView.setBinaryData(answer.key, files[stringVal] as VaultFile)
            } else {
                if (stringVal != null) {
                    formView.setBinaryData(answer.key, stringVal)
                }
            }
        }
    }

    private fun fillAnswersToForm(instance: UwaziEntityInstance, formView: UwaziFormView) {
        //TODO REFACTOR THIS INTO A SEPARATE PARSER
        val files = mutableMapOf<String, FormMediaFile>()
        for (file in instance.widgetMediaFiles) {
            files[file.name] = file
        }

        formView.setBinaryData(UWAZI_TITLE, instance.title)

        for (answer in instance.metadata) {
            if ((answer.value as List<*>).size > 1) {
                formView.setBinaryData(answer.key, answer.value)
            } else {
                val uwaziValue: UwaziValue = answer.value[0] as UwaziValue
                val stringVal = uwaziValue.value
                if (files.containsKey(stringVal)) {
                    formView.setBinaryData(answer.key, files[stringVal] as VaultFile)
                } else {
                    formView.setBinaryData(answer.key, stringVal)
                }
            }
        }
    }

    private fun parseUwaziInstance() {
        prepareFormView()
        putAnswersToForm(entityInstance, uwaziFormView)
    }

    private fun parseUwaziTemplate() {
        prepareFormView()
        fillAnswersToForm(entityInstance, uwaziFormView)
    }

    private fun prepareFormView() {
        //TODO REFACTOR THIS INTO A SEPARATE PARSER
        entryPrompts.clear()

        //TODO Handle this special common props smarter
        entryPrompts.add(uwaziPdfsPrompt)
        entryPrompts.add(uwaziFilesPrompt)

        if (template?.entityRow?.commonProperties?.get(0)?.translatedLabel?.length!! > 0) {
            uwaziTitlePrompt.question =
                template?.entityRow?.commonProperties?.get(0)?.translatedLabel
        }
        entryPrompts.add(uwaziTitlePrompt)

        for (property in template?.entityRow?.properties!!) {
            val entryPrompt = UwaziEntryPrompt(
                property.name,
                property.id,
                property.type,
                property.translatedLabel,
                property.required,
                property.translatedLabel
            )
            if (property.values != null) {
                entryPrompt.selectValues = property.values
            }
            entryPrompts.add(entryPrompt)
        }

        val arr: Array<UwaziEntryPrompt?> = arrayOfNulls(entryPrompts.size)
        arr.indices.forEach { i ->
            arr[i] = entryPrompts[i]
        }
        uwaziFormView = UwaziFormView(requireContext(), arr)
        screenView?.addView(uwaziFormView)
    }

    private fun putVaultFileInForm(vaultFile: String) {
        vaultFile.let { uwaziFormView.setBinaryData(it) }
    }

    private fun putLocationInForm(location: UwaziGeoData) {
        uwaziFormView.setBinaryData(location)
    }

    private fun showSavedDialog() {
        DialogUtils.showBottomMessage(
            activity,
            getString(R.string.Uwazi_EntryInstance_SavedInfo),
            false
        )
    }

    private fun showValidationMandatoryFieldsDialog() {
        DialogUtils.showBottomMessage(
            activity,
            getString(R.string.Uwazi_Entry_Validation_MandatoryFields),
            false
        )
    }

    private fun showValidationErrorsFieldsDialog() {
        DialogUtils.showBottomMessage(
            activity,
            getString(R.string.collect_form_toast_validation_generic_error),
            false
        )
    }

    private fun showValidationMandatoryTitleDialog() {
        DialogUtils.showBottomMessage(
            activity,
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
                        activity.changeTemporaryTimeout()
                        requestGpsPermissions(C.GPS_PROVIDER)
                    }
                }
            })
    }
}