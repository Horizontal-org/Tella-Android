package org.horizontal.tella.mobile.views.fragment.uwazi.entry

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import com.google.gson.Gson
import com.hzontal.tella_vault.MyLocation
import org.hzontal.shared_ui.bottomsheet.BottomSheetUtils
import org.hzontal.shared_ui.utils.DialogUtils
import org.horizontal.tella.mobile.MyApplication
import org.horizontal.tella.mobile.R
import org.horizontal.tella.mobile.bus.EventObserver
import org.horizontal.tella.mobile.bus.event.LocationPermissionRequiredEvent
import org.horizontal.tella.mobile.data.uwazi.UwaziConstants
import org.horizontal.tella.mobile.data.uwazi.UwaziConstants.UWAZI_RELATION_SHIP_ENTITIES
import org.horizontal.tella.mobile.databinding.UwaziEntryFragmentBinding
import org.horizontal.tella.mobile.domain.entity.EntityStatus
import org.horizontal.tella.mobile.domain.entity.uwazi.CollectTemplate
import org.horizontal.tella.mobile.presentation.uwazi.UwaziGeoData
import org.horizontal.tella.mobile.presentation.uwazi.UwaziRelationShipEntity
import org.horizontal.tella.mobile.util.C
import org.horizontal.tella.mobile.views.activity.LocationMapActivity
import org.horizontal.tella.mobile.views.base_ui.BaseBindingFragment
import org.horizontal.tella.mobile.views.fragment.uwazi.SharedLiveData
import org.horizontal.tella.mobile.views.fragment.uwazi.attachments.VAULT_FILE_KEY
import org.horizontal.tella.mobile.views.fragment.uwazi.send.SEND_ENTITY
import org.horizontal.tella.mobile.views.fragment.uwazi.viewpager.DRAFT_LIST_PAGE_INDEX
import org.horizontal.tella.mobile.views.fragment.uwazi.viewpager.OUTBOX_LIST_PAGE_INDEX
import org.horizontal.tella.mobile.views.fragment.uwazi.viewpager.SUBMITTED_LIST_PAGE_INDEX
import org.horizontal.tella.mobile.views.fragment.uwazi.widgets.OnEntityClickInEntryListener
import org.horizontal.tella.mobile.views.fragment.vault.attachements.OnNavBckListener


const val COLLECT_TEMPLATE = "collect_template"
const val UWAZI_INSTANCE = "uwazi_instance"
const val UWAZI_TITLE = "title"
const val UWAZI_SUPPORTING_FILES = "supporting_files"
const val UWAZI_PRIMARY_DOCUMENTS = "primary_documents"
const val BUNDLE_IS_FROM_UWAZI_ENTRY = "bundle_is_from_uwazi_entry"
const val UWAZI_TEMPLATE = "uwazi_template"
const val UWAZI_ENTRY_PROMPT_ID = "uwazi_entry_prompt_id"
const val UWAZI_ENTRY_PROMPT_TITLE = "uwazi_entry_prompt_title"
const val UWAZI_SELECTED_ENTITIES = "uwazi_selected_entities"

class UwaziEntryFragment :
    BaseBindingFragment<UwaziEntryFragmentBinding>(UwaziEntryFragmentBinding::inflate),
    OnNavBckListener, OnEntityClickInEntryListener {

    private val viewModel: SharedUwaziSubmissionViewModel by viewModels()

    private val uwaziParser: UwaziParser by lazy { UwaziParser(context) }
    private var screenView: ViewGroup? = null
    private lateinit var uwaziFormView: UwaziFormView
    private var result: List<CollectTemplate> = ArrayList()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.refreshEntitiesList()
        initObservers()
        initView()
        setupFragmentResultListener()
    }

    private fun setupFragmentResultListener() {
        parentFragmentManager.setFragmentResultListener(
            UwaziConstants.UWAZI_RELATION_SHIP_REQUEST_KEY,
            viewLifecycleOwner
        ) { requestKey, bundle ->
            handleRelationshipEntitiesResult(requestKey, bundle)
        }
    }

    private fun handleRelationshipEntitiesResult(requestKey: String, bundle: Bundle) {
        if (requestKey == UwaziConstants.UWAZI_RELATION_SHIP_REQUEST_KEY && bundle.containsKey(
                UWAZI_RELATION_SHIP_ENTITIES
            )
        ) {
            val resultReceived = bundle.getString(UWAZI_RELATION_SHIP_ENTITIES) ?: ""
            putRelationShipEntitiesInForm(resultReceived)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == C.MEDIA_FILE_ID && resultCode == Activity.RESULT_OK) {
            val vaultFile = data?.getStringExtra(VAULT_FILE_KEY) ?: ""
            putVaultFileInForm(vaultFile)
        }

        if (requestCode == C.SELECTED_LOCATION && resultCode == Activity.RESULT_OK) {
            if (data != null) {
                val myLocation: MyLocation =
                    data.getSerializableExtra(LocationMapActivity.SELECTED_LOCATION) as MyLocation
                putLocationInForm(UwaziGeoData("", myLocation.latitude, myLocation.longitude))
            }
        }
    }

    private fun initView() {
        binding.apply {
            toolbar.backClickListener = { onBackPressed() }
            toolbar.onRightClickListener = {
                uwaziParser.setInstanceStatus(EntityStatus.DRAFT)
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

        if (arguments?.getString(UWAZI_INSTANCE) != null) {
            arguments?.getString(UWAZI_INSTANCE)?.let { parseUwaziInstance(it) }
        }

        if (arguments?.getString(COLLECT_TEMPLATE) != null) {
            arguments?.getString(COLLECT_TEMPLATE)?.let {
                parseUwaziTemplate(it)
            }
        }
        binding.toolbar.setStartTextTitle(uwaziParser.getTemplate()?.entityRow?.translatedName.toString())
    }

    private fun initObservers() {
        with(viewModel) {
            progress.observe(viewLifecycleOwner) { status ->
                when (status) {
                    EntityStatus.SUBMISSION_PENDING, EntityStatus.SUBMISSION_ERROR -> {
                        SharedLiveData.updateViewPagerPosition.postValue(OUTBOX_LIST_PAGE_INDEX)
                        nav().popBackStack()
                        progress.postValue(EntityStatus.UNKNOWN)
                    }

                    EntityStatus.SUBMITTED -> {
                        SharedLiveData.updateViewPagerPosition.postValue(SUBMITTED_LIST_PAGE_INDEX)
                        nav().popBackStack()
                        progress.postValue(EntityStatus.UNKNOWN)
                    }

                    EntityStatus.DRAFT -> {
                        SharedLiveData.updateViewPagerPosition.postValue(DRAFT_LIST_PAGE_INDEX)
                        nav().popBackStack()
                        showSavedDialog()
                        progress.postValue(EntityStatus.UNKNOWN)
                    }

                    else -> {}
                }
            }
            templates.observe(viewLifecycleOwner) { list ->
                result = list.templates
                result = result.filter { (it.id.equals(uwaziParser.getTemplate()?.id)) }
                if (!result.isEmpty()) uwaziParser.setTemplate(result.get(0))
            }
            progressRefresh.observe(viewLifecycleOwner) {
                binding.progressCircular.isVisible = it
            }
        }
    }

    private fun sendEntity() {
        if (!uwaziParser.getAnswersFromForm(true, uwaziFormView)) {
            uwaziFormView.setFocus(context)
            //showValidationMandatoryFieldsDialog()
            showValidationErrorsFieldsDialog()
        } else {
            bundle.putString(SEND_ENTITY, uwaziParser.getGsonTemplate())
            bundle.putBoolean(BUNDLE_IS_FROM_UWAZI_ENTRY, true)
            navManager().navigateFromUwaziEntryToSendScreen()
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

    private fun putRelationShipEntitiesInForm(entitiesList: String) {
        uwaziFormView.setBinaryData(entitiesList)
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
                    uwaziParser.setInstanceStatus(EntityStatus.DRAFT)
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

    override fun onSelectEntitiesClickedInEntryFragment(
        formEntryPrompt: UwaziEntryPrompt,
        entitiesNames: MutableList<UwaziRelationShipEntity>
    ) {
        bundle.apply {
            putString(UWAZI_TEMPLATE, uwaziParser.getToGsonTemplate())
            putString(UWAZI_ENTRY_PROMPT_ID, formEntryPrompt.index.toString())
            putString(UWAZI_ENTRY_PROMPT_TITLE, formEntryPrompt.longText.toString())
            putString(UWAZI_SELECTED_ENTITIES, Gson().toJson(entitiesNames))
        }
        navManager().navigateFromUwaziEntryToSelectEntities()
    }

}