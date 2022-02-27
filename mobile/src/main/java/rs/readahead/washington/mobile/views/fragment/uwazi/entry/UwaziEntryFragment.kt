package rs.readahead.washington.mobile.views.fragment.uwazi.entry

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.NavHostFragment
import com.google.gson.Gson
import com.google.gson.internal.LinkedTreeMap
import com.google.gson.reflect.TypeToken
import com.hzontal.tella_vault.MyLocation
import com.hzontal.tella_vault.VaultFile
import org.hzontal.shared_ui.utils.DialogUtils
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.data.uwazi.UwaziConstants
import rs.readahead.washington.mobile.databinding.UwaziEntryFragmentBinding
import rs.readahead.washington.mobile.domain.entity.collect.FormMediaFile
import rs.readahead.washington.mobile.domain.entity.uwazi.CollectTemplate
import rs.readahead.washington.mobile.domain.entity.uwazi.UwaziEntityInstance
import rs.readahead.washington.mobile.domain.entity.uwazi.UwaziEntityStatus
import rs.readahead.washington.mobile.presentation.uwazi.UwaziGeoData
import rs.readahead.washington.mobile.presentation.uwazi.UwaziValue
import rs.readahead.washington.mobile.util.C
import rs.readahead.washington.mobile.views.activity.LocationMapActivity
import rs.readahead.washington.mobile.views.base_ui.BaseFragment
import rs.readahead.washington.mobile.views.fragment.uwazi.SharedLiveData
import rs.readahead.washington.mobile.views.fragment.uwazi.attachments.VAULT_FILE_KEY
import rs.readahead.washington.mobile.views.fragment.uwazi.send.SEND_ENTITY
import rs.readahead.washington.mobile.views.fragment.uwazi.viewpager.OUTBOX_LIST_PAGE_INDEX
import rs.readahead.washington.mobile.views.fragment.uwazi.viewpager.SUBMITTED_LIST_PAGE_INDEX
import rs.readahead.washington.mobile.views.fragment.vault.attachements.OnNavBckListener


const val COLLECT_TEMPLATE = "collect_template"
const val UWAZI_INSTANCE = "uwazi_instance"
const val UWAZI_TITLE = "title"
const val UWAZI_SUPPORTING_FILES = "supporting_files"
const val UWAZI_PRIMARY_DOCUMENTS = "primary_documents"

class UwaziEntryFragment : BaseFragment(), OnNavBckListener {
    private val viewModel: SharedUwaziSubmissionViewModel by lazy {
        ViewModelProvider(activity).get(SharedUwaziSubmissionViewModel::class.java)
    }
    private var binding: UwaziEntryFragmentBinding? = null
    private var template: CollectTemplate? = null
    private var entityInstance: UwaziEntityInstance = UwaziEntityInstance()
    private val bundle by lazy { Bundle() }
    private var screenView: ViewGroup? = null
    private var entryPrompts = mutableListOf<UwaziEntryPrompt>()
    private lateinit var uwaziFormView: UwaziFormView
    var hasInitializedRootView = false
    private var rootView: View? = null

    private val uwaziTitlePrompt = UwaziEntryPrompt(
        UWAZI_TITLE,
        "10242048",
        UwaziConstants.UWAZI_DATATYPE_TEXT,
        "Title",
        true,
        "Enter the submission title"
    )

    private val uwaziFilesPrompt = UwaziEntryPrompt(
        UWAZI_SUPPORTING_FILES,
        "10242049",
        UwaziConstants.UWAZI_DATATYPE_MULTIFILES,
        "Supporting files",
        false,
        "Select as many files as you wish"
    )

    private val uwaziPdfsPrompt = UwaziEntryPrompt(
        UWAZI_PRIMARY_DOCUMENTS,
        "10242050",
        UwaziConstants.UWAZI_DATATYPE_MULTIPDFFILES,
        "Primary documents",
        false,
        "Attach as many PDF files as you wish"
    )

    private fun getPersistentView(inflater: LayoutInflater, container: ViewGroup?): View {
        if (binding == null) {
            // Inflate the layout for this fragment
            binding = UwaziEntryFragmentBinding.inflate(inflater, container, false)
        } else {
            // Do not inflate the layout again.
            // The returned View of onCreateView will be added into the fragment.
            // However it is not allowed to be added twice even if the parent is same.
            // So we must remove rootView from the existing parent view group
            // (it will be added back).
            (rootView?.parent as? ViewGroup)?.removeView(binding!!.root)
        }

        return binding!!.root
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return getPersistentView(inflater, container)

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
            val vaultFile = data?.getStringExtra(VAULT_FILE_KEY)  ?: ""
            getUwaziFormFiles(vaultFile)
        }

        if (requestCode == C.SELECTED_LOCATION && resultCode == Activity.RESULT_OK) {
            val myLocation: MyLocation =
                data!!.getSerializableExtra(LocationMapActivity.SELECTED_LOCATION) as MyLocation
            putLocationInForm(UwaziGeoData("", myLocation.latitude, myLocation.longitude))
        }
    }

    override fun initView(view: View) {
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

        if (!hasGpsPermissions(requireContext())) {
            requestGpsPermissions(C.GPS_PROVIDER)
        }
    }

    private fun initView() {
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
                        showSavedDialog()
                        progress.postValue(UwaziEntityStatus.UNKNOWN)
                    }
                }
            })

            attachedFiles.observe(viewLifecycleOwner, {
                putVaultFileInForm(Gson().toJson(it))
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
            showValidationMandatoryFieldsDialog()
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

        // check required fields
        if (answers[UWAZI_TITLE] == null) {
            uwaziFormView.setValidationConstraintText(
                UWAZI_TITLE,
                getString(R.string.collect_form_error_response_mandatory)
            )
            validationRequired = true
        }
        if (isSend) {
            for (property in template?.entityRow?.properties!!) {
                if (property.required && (answers[property.name] == null)) {
                    uwaziFormView.setValidationConstraintText(
                        property.name,
                        getString(R.string.collect_form_error_response_mandatory)
                    )
                    validationRequired = true
                }
            }
        }
        if (validationRequired) return false

        // put answers to entity
        for (answer in answers) {
            if (answer.value != null) {
                if (answer.key == UWAZI_TITLE) {
                    entityInstance.title = (answer.value as UwaziValue).value as String
                } else {
                    if (answer.value is List<*>) {

                        hashmap[answer.key] = (answer.value) as List<UwaziValue>

                    } else {
                        hashmap[answer.key] = arrayListOf(UwaziValue((answer.value as UwaziValue).value))
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

        if (template?.entityRow?.commonProperties?.get(0)?.translatedLabel?.length!! > 0){
            uwaziTitlePrompt.question = template?.entityRow?.commonProperties?.get(0)?.translatedLabel
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

    private fun getUwaziFormFiles(idList: String) {
        val fileIds: Array<String> = emptyArray()

        val files = Gson().fromJson<java.util.ArrayList<String>>(
            idList as String?,
            object : TypeToken<List<String?>?>() {}.type
        )
        for (s in files) {
            fileIds.plus(s)
        }

        viewModel.getFiles(fileIds)
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
}