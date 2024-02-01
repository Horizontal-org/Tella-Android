package rs.readahead.washington.mobile.views.fragment.reports.entry

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.hzontal.tella_locking_ui.common.extensions.onChange
import com.hzontal.tella_vault.VaultFile
import com.hzontal.tella_vault.filter.FilterType
import com.proxym.shared.widget.dropdown_list.CustomDropdownItemClickListener
import dagger.hilt.android.AndroidEntryPoint
import org.hzontal.shared_ui.bottomsheet.BottomSheetUtils
import org.hzontal.shared_ui.bottomsheet.VaultSheetUtils.IVaultFilesSelector
import org.hzontal.shared_ui.bottomsheet.VaultSheetUtils.showVaultSelectFilesSheet
import org.hzontal.shared_ui.dropdownlist.DropDownItem
import org.hzontal.shared_ui.utils.DialogUtils
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.databinding.FragmentReportsEntryBinding
import rs.readahead.washington.mobile.domain.entity.EntityStatus
import rs.readahead.washington.mobile.domain.entity.reports.ReportInstance
import rs.readahead.washington.mobile.domain.entity.reports.TellaReportServer
import rs.readahead.washington.mobile.media.MediaFileHandler
import rs.readahead.washington.mobile.util.C
import rs.readahead.washington.mobile.util.hide
import rs.readahead.washington.mobile.util.invisible
import rs.readahead.washington.mobile.util.show
import rs.readahead.washington.mobile.views.activity.camera.CameraActivity
import rs.readahead.washington.mobile.views.activity.camera.CameraActivity.Companion.CAPTURE_WITH_AUTO_UPLOAD
import rs.readahead.washington.mobile.views.adapters.reports.ReportsFilesRecyclerViewAdapter
import rs.readahead.washington.mobile.views.base_ui.BaseBindingFragment
import rs.readahead.washington.mobile.views.fragment.REPORT_ENTRY
import rs.readahead.washington.mobile.views.fragment.reports.ReportsViewModel
import rs.readahead.washington.mobile.views.fragment.reports.viewpager.OUTBOX_LIST_PAGE_INDEX
import rs.readahead.washington.mobile.views.fragment.uwazi.SharedLiveData
import rs.readahead.washington.mobile.views.fragment.uwazi.attachments.AttachmentsActivitySelector
import rs.readahead.washington.mobile.views.fragment.uwazi.attachments.VAULT_FILES_FILTER
import rs.readahead.washington.mobile.views.fragment.uwazi.attachments.VAULT_FILE_KEY
import rs.readahead.washington.mobile.views.fragment.uwazi.attachments.VAULT_PICKER_SINGLE
import rs.readahead.washington.mobile.views.interfaces.IReportAttachmentsHandler

const val BUNDLE_REPORT_FORM_INSTANCE = "bundle_report_form_instance"
const val BUNDLE_REPORT_VAULT_FILE = "bundle_report_vault_file"
const val BUNDLE_REPORT_AUDIO = "bundle_report_audio"
const val BUNDLE_IS_FROM_DRAFT = "bundle_is_from_draft"

@AndroidEntryPoint
class ReportsEntryFragment :
    BaseBindingFragment<FragmentReportsEntryBinding>(FragmentReportsEntryBinding::inflate),
    IReportAttachmentsHandler, CustomDropdownItemClickListener {
    private val viewModel by viewModels<ReportsViewModel>()
    private lateinit var gridLayoutManager: GridLayoutManager
    private val filesRecyclerViewAdapter: ReportsFilesRecyclerViewAdapter by lazy {
        ReportsFilesRecyclerViewAdapter(this)
    }
    private lateinit var selectedServer: TellaReportServer
    private lateinit var servers: ArrayList<TellaReportServer>
    private var reportInstance: ReportInstance? = null
    private var isNewDraft = true
    private var isTitleEnabled = false
    private var isDescriptionEnabled = false
    private var isServerSelected = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setFragmentResultListener(BUNDLE_REPORT_AUDIO) { _, bundle ->
            val file = bundle.get(BUNDLE_REPORT_VAULT_FILE) as VaultFile
            bundle.remove(BUNDLE_REPORT_VAULT_FILE)
            putFiles(listOf(file))
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        initView()
        initData()
    }

    private fun initView() {
        gridLayoutManager = GridLayoutManager(context, 3)
        binding.filesRecyclerView.apply {
            layoutManager = gridLayoutManager
            adapter = filesRecyclerViewAdapter
        }
        binding.toolbar.backClickListener = {
            exitOrSave()
        }

        arguments?.let { bundle ->
            if (bundle.get(BUNDLE_REPORT_FORM_INSTANCE) != null) {
                reportInstance = bundle.get(BUNDLE_REPORT_FORM_INSTANCE) as ReportInstance
                bundle.remove(BUNDLE_REPORT_FORM_INSTANCE)
            }
        }

        reportInstance?.let { instance ->
            binding.reportTitleEt.setText(instance.title)
            binding.reportDescriptionEt.setText(instance.description)
            putFiles(viewModel.mediaFilesToVaultFiles(instance.widgetMediaFiles))
            isNewDraft = false
        }
        highLightButtonsInit()
        checkIsNewDraftEntry()
    }

    private fun exitOrSave() {
        val title = binding.reportTitleEt.text.toString()
        val description = binding.reportDescriptionEt.text.toString()
        if (reportInstance == null && title.isEmpty()) {
            nav().popBackStack()
        } else if (reportInstance == null && title.isNotEmpty()) {
            showConfirmSaveOrExit(reportInstance)
        } else if (reportInstance != null && (reportInstance?.title != title || reportInstance?.description != description
                    || filesRecyclerViewAdapter.getFiles() != viewModel.mediaFilesToVaultFiles(
                reportInstance?.widgetMediaFiles
            ))
        ) {
            showConfirmSaveOrExit(reportInstance)
        } else {
            nav().popBackStack()
        }
    }

    private fun showConfirmSaveOrExit(reportInstance: ReportInstance?) {
        BottomSheetUtils.showConfirmSheet(
            baseActivity.supportFragmentManager,
            getString(R.string.Report_Dialog_Draft_Title),
            getString(R.string.collect_form_exit_dialog_expl),
            getString(R.string.collect_form_exit_dialog_action_save_exit),
            getString(R.string.collect_form_exit_dialog_action_exit_anyway),
            object : BottomSheetUtils.ActionConfirmed {
                override fun accept(isConfirmed: Boolean) {
                    if (isConfirmed) {
                        if (reportInstance == null) {
                            saveReportAsDraft(true)
                        } else {
                            if (reportInstance.status == EntityStatus.DRAFT) {
                                saveReportAsDraft(true)
                            } else {
                                saveReportAsOutbox()
                            }
                        }
                    } else {
                        nav().popBackStack()
                    }
                }
            }
        )
    }

    private fun highLightButtonsInit() {
        binding.reportTitleEt.let { title ->
            isTitleEnabled = title.length() > 0
        }

        binding.reportDescriptionEt.let { description ->
            isDescriptionEnabled = description.length() > 0
        }

        binding.reportTitleEt.onChange { title ->
            isTitleEnabled = title.isNotEmpty()
            highLightButtons()
        }

        binding.reportDescriptionEt.onChange { description ->
            isDescriptionEnabled = description.isNotEmpty()
            highLightButtons()
        }

        binding.deleteBtn.setOnClickListener {
            reportInstance?.let { instance -> showDeleteBottomSheet(instance) }
        }
    }

    private fun highLightButtons() {
        val isSubmitEnabled =
            isTitleEnabled && isServerSelected && (isDescriptionEnabled || filesRecyclerViewAdapter.getFiles()
                .isNotEmpty())

        val disabled: Float = context?.getString(R.string.alpha_disabled)?.toFloat() ?: 1.0f
        val enabled: Float = context?.getString(R.string.alpha_enabled)?.toFloat() ?: 1.0f

        binding.sendReportBtn.setBackgroundResource(if (isSubmitEnabled) R.drawable.bg_round_orange_btn else R.drawable.bg_round_orange16_btn)
        binding.sendLaterBtn.alpha = (if (isSubmitEnabled) enabled else disabled)
        binding.sendReportBtn.alpha = (if (isSubmitEnabled) enabled else disabled)

        binding.sendLaterBtn.setOnClickListener {
            if (isSubmitEnabled) {
                saveReportAsOutbox()
            } else {
                showSubmitReportErrorSnackBar()
            }
        }

        binding.sendReportBtn.setOnClickListener {
            if (isSubmitEnabled) {
                saveReportAsPending()
            } else {
                showSubmitReportErrorSnackBar()
            }
        }

        if (isTitleEnabled && isServerSelected) {
            binding.toolbar.onRightClickListener = {
                saveReportAsDraft(false)
            }
        } else {
            binding.toolbar.onRightClickListener = {}
        }
    }

    private fun showSubmitReportErrorSnackBar() {
        val errorRes =
            if (servers.size > 1) R.string.Snackbar_Submit_Report_WithProject_Error else R.string.Snackbar_Submit_Report_Error

        DialogUtils.showBottomMessage(
            baseActivity,
            getString(errorRes),
            false
        )
    }

    private fun saveReportAsDraft(exitAfterSave: Boolean) {
        viewModel.saveDraft(
            viewModel.getDraftFormInstance(
                id = reportInstance?.id,
                title = binding.reportTitleEt.text.toString(),
                description = binding.reportDescriptionEt.text.toString(),
                files = viewModel.vaultFilesToMediaFiles(filesRecyclerViewAdapter.getFiles()),
                server = selectedServer
            ),
            exitAfterSave
        )
    }

    private fun saveReportAsOutbox() {
        viewModel.saveOutbox(
            viewModel.getFormInstance(
                id = reportInstance?.id,
                title = binding.reportTitleEt.text.toString(),
                description = binding.reportDescriptionEt.text.toString(),
                files = viewModel.vaultFilesToMediaFiles(filesRecyclerViewAdapter.getFiles()),
                server = selectedServer,
                status = EntityStatus.FINALIZED
            )
        )
    }

    private fun saveReportAsPending() {
        viewModel.saveOutbox(
            viewModel.getFormInstance(
                id = reportInstance?.id,
                title = binding.reportTitleEt.text.toString(),
                description = binding.reportDescriptionEt.text.toString(),
                files = viewModel.vaultFilesToMediaFiles(filesRecyclerViewAdapter.getFiles()),
                server = selectedServer,
                status = EntityStatus.SUBMISSION_PARTIAL_PARTS
            )
        )
    }

    private fun initData() {
        with(viewModel) {
            listServers()
            serversList.observe(viewLifecycleOwner) { serversList ->
                servers = arrayListOf()
                servers.addAll(serversList)
                if (serversList.size > 1) {
                    val listDropDown = mutableListOf<DropDownItem>()
                    serversList.map { server ->
                        listDropDown.add(DropDownItem(server.projectId, server.projectName))
                    }
                    binding.dropdownGroup.show()
                    binding.serversDropdown.setListAdapter(
                        listDropDown,
                        this@ReportsEntryFragment,
                        baseActivity
                    )
                    this@ReportsEntryFragment.reportInstance?.let {
                        servers.first { server -> server.id == it.serverId }.let {
                            selectedServer = it
                            isServerSelected = true
                            binding.serversDropdown.setDefaultName(it.name)
                            highLightButtons()
                        }
                    }
                } else {
                    binding.dropdownGroup.hide()
                    selectedServer = serversList[0]
                    isServerSelected = true
                    highLightButtons()
                }
            }
            reportInstance.observe(viewLifecycleOwner) { instance ->
                when (instance.status) {
                    EntityStatus.DRAFT -> {
                        this@ReportsEntryFragment.reportInstance = instance
                        DialogUtils.showBottomMessage(
                            baseActivity, getString(R.string.Reports_Saved_Draft),
                            false
                        )
                    }

                    EntityStatus.SUBMISSION_PARTIAL_PARTS -> {
                        this@ReportsEntryFragment.submitReport(instance)
                    }

                    EntityStatus.FINALIZED -> {
                        DialogUtils.showBottomMessage(
                            baseActivity, getString(R.string.Report_Save_Outbox_Confirmation),
                            false
                        )
                        nav().popBackStack()
                        SharedLiveData.updateViewPagerPosition.postValue(OUTBOX_LIST_PAGE_INDEX)
                    }

                    else -> {}
                }
            }
            exitAfterSave.observe(viewLifecycleOwner) { exitAfterSave ->
                if (exitAfterSave) {
                    nav().popBackStack()
                }
            }
            instanceDeleted.observe(viewLifecycleOwner) {
                nav().popBackStack()
            }
        }
    }

    private fun showSelectFilesSheet() {
        showVaultSelectFilesSheet(baseActivity.supportFragmentManager,
            baseActivity.getString(R.string.Uwazi_WidgetMedia_Take_Photo),
            baseActivity.getString(R.string.Vault_RecordAudio_SheetAction),
            baseActivity.getString(R.string.Uwazi_WidgetMedia_Select_From_Device),
            baseActivity.getString(R.string.Uwazi_WidgetMedia_Select_From_Tella),
            null,
            baseActivity.getString(R.string.Uwazi_MiltiFileWidget_SelectFiles),
            object : IVaultFilesSelector {
                override fun importFromVault() {
                    showAttachmentsActivity()
                }

                override fun goToRecorder() {
                    showAudioRecorderActivity()
                }

                override fun goToCamera() {
                    showCameraActivity()
                }

                override fun importFromDevice() {
                    importMedia()
                }
            })
    }

    private fun checkIsNewDraftEntry() {
        if (isNewDraft) {
            binding.deleteBtn.invisible()
            binding.sendLaterBtn.show()
            binding.sendReportBtn.text = getString(R.string.collect_end_action_submit)
        } else {
            binding.deleteBtn.show()
            binding.sendLaterBtn.invisible()
            binding.sendReportBtn.text = getString(R.string.Send_Action_Label)
        }
    }

    private fun showAttachmentsActivity() {
        try {
            baseActivity.startActivityForResult(
                Intent(activity, AttachmentsActivitySelector::class.java)
                    // .putExtra(VAULT_FILE_KEY, Gson().toJson(ids))
                    .putExtra(
                        VAULT_FILES_FILTER, FilterType.ALL_WITHOUT_DIRECTORY
                    ).putExtra(VAULT_PICKER_SINGLE, false), C.MEDIA_FILE_ID
            )
        } catch (e: Exception) {
            FirebaseCrashlytics.getInstance().recordException(e)
        }
    }

    private fun showCameraActivity() {
        try {
            val intent = Intent(context, CameraActivity::class.java)
            intent.apply {
                putExtra(CameraActivity.INTENT_MODE, CameraActivity.IntentMode.COLLECT.name)
                putExtra(CAPTURE_WITH_AUTO_UPLOAD, false)
            }

            baseActivity.startActivityForResult(intent, C.MEDIA_FILE_ID)
        } catch (e: java.lang.Exception) {
            FirebaseCrashlytics.getInstance().recordException(e)
        }
    }

    private fun importMedia() {
        baseActivity.maybeChangeTemporaryTimeout {
            MediaFileHandler.startSelectMediaActivity(
                activity, "image/* video/* audio/*",
                arrayOf("image/*", "video/*", "audio/*"), C.IMPORT_FILE
            )
        }
    }

    private fun showAudioRecorderActivity() {
        try {
            bundle.putBoolean(REPORT_ENTRY, true)
            this.navManager().navigateToMicro()
        } catch (e: java.lang.Exception) {
            FirebaseCrashlytics.getInstance().recordException(e)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == C.MEDIA_FILE_ID && resultCode == Activity.RESULT_OK) {
            val vaultFile = data?.getStringExtra(VAULT_FILE_KEY) ?: ""
            putFiles(viewModel.putVaultFilesInForm(vaultFile))
        }
    }

    private fun putFiles(vaultFileList: List<VaultFile>) {
        for (file in vaultFileList) {
            filesRecyclerViewAdapter.insertAttachment(file)
        }
        binding.filesRecyclerView.visibility = View.VISIBLE
        highLightButtons()
    }

    override fun playMedia(mediaFile: VaultFile?) {

    }

    override fun addFiles() {
        showSelectFilesSheet()
    }

    override fun removeFiles() {
        highLightButtons()
    }

    override fun onDropDownItemClicked(position: Int, chosenItem: DropDownItem) {
        binding.serversDropdown.setDefaultName(chosenItem.name)
        servers.get(position).let {
            selectedServer = it
            isServerSelected = true
        }
        highLightButtons()
    }

    private fun submitReport(reportInstance: ReportInstance) {
        val bundle = Bundle()
        bundle.putSerializable(BUNDLE_REPORT_FORM_INSTANCE, reportInstance)
        bundle.putBoolean(BUNDLE_IS_FROM_DRAFT, true)
        navManager().navigateFromNewReportsScreenToReportSendScreen()
    }

    private fun showDeleteBottomSheet(reportInstance: ReportInstance) {
        BottomSheetUtils.showConfirmSheet(
            baseActivity.supportFragmentManager,
            reportInstance.title,
            getString(R.string.Collect_DeleteDraftForm_SheetExpl),
            getString(R.string.action_yes),
            getString(R.string.action_no), consumer = object : BottomSheetUtils.ActionConfirmed {
                override fun accept(isConfirmed: Boolean) {
                    if (isConfirmed) {
                        viewModel.deleteReport(reportInstance)
                    }
                }
            })
    }
}