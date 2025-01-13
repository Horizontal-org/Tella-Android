package rs.readahead.washington.mobile.views.fragment.main_connexions.base

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.hzontal.tella_locking_ui.common.extensions.onChange
import com.hzontal.tella_vault.VaultFile
import com.hzontal.tella_vault.filter.FilterType
import com.proxym.shared.widget.dropdown_list.CustomDropdownItemClickListener
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.hzontal.shared_ui.bottomsheet.BottomSheetUtils
import org.hzontal.shared_ui.bottomsheet.VaultSheetUtils.IVaultFilesSelector
import org.hzontal.shared_ui.bottomsheet.VaultSheetUtils.showVaultSelectFilesSheet
import org.hzontal.shared_ui.dropdownlist.DropDownItem
import org.hzontal.shared_ui.utils.DialogUtils
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.databinding.FragmentReportsEntryBinding
import rs.readahead.washington.mobile.domain.entity.EntityStatus
import rs.readahead.washington.mobile.domain.entity.Server
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
import rs.readahead.washington.mobile.views.fragment.recorder.REPORT_ENTRY
import rs.readahead.washington.mobile.views.fragment.uwazi.attachments.AttachmentsActivitySelector
import rs.readahead.washington.mobile.views.fragment.uwazi.attachments.VAULT_FILES_FILTER
import rs.readahead.washington.mobile.views.fragment.uwazi.attachments.VAULT_FILE_KEY
import rs.readahead.washington.mobile.views.fragment.uwazi.attachments.VAULT_PICKER_SINGLE
import rs.readahead.washington.mobile.views.interfaces.IReportAttachmentsHandler

const val BUNDLE_REPORT_FORM_INSTANCE = "bundle_report_form_instance"
const val BUNDLE_REPORT_VAULT_FILE = "bundle_report_vault_file"
const val BUNDLE_REPORT_AUDIO = "bundle_report_audio"
const val BUNDLE_IS_FROM_DRAFT = "bundle_is_from_draft"

abstract class BaseReportsEntryFragment :
    BaseBindingFragment<FragmentReportsEntryBinding>(FragmentReportsEntryBinding::inflate),
    IReportAttachmentsHandler, CustomDropdownItemClickListener, OnNavBckListener {
    protected abstract val viewModel: BaseReportsViewModel // Child classes provide the specific ViewModel
    private lateinit var gridLayoutManager: GridLayoutManager
    private val filesRecyclerViewAdapter: ReportsFilesRecyclerViewAdapter by lazy {
        ReportsFilesRecyclerViewAdapter(this)
    }
    private lateinit var selectedServer: Server
    private lateinit var servers: ArrayList<Server>
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
        when {
            reportInstance == null && title.isEmpty() -> {
                nav().popBackStack()
            }

            reportInstance == null && title.isNotEmpty() -> {
                showConfirmSaveOrExit(reportInstance)
            }

            reportInstance != null && (reportInstance?.title != title || reportInstance?.description != description
                    || filesRecyclerViewAdapter.getFiles() != viewModel.mediaFilesToVaultFiles(
                reportInstance?.widgetMediaFiles
            )) -> {
                showConfirmSaveOrExit(reportInstance)
            }

            else -> {
                nav().popBackStack()
            }
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
        binding.apply {
            reportTitleEt.let { title ->
                isTitleEnabled = title.length() > 0
            }

            reportDescriptionEt.let { description ->
                isDescriptionEnabled = description.length() > 0
            }

            reportTitleEt.onChange { title ->
                isTitleEnabled = title.isNotEmpty()
                highLightButtons()
            }

            reportDescriptionEt.onChange { description ->
                isDescriptionEnabled = description.isNotEmpty()
                highLightButtons()
            }

            deleteBtn.setOnClickListener {
                reportInstance?.let { instance -> showDeleteBottomSheet(instance) }
            }
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

        initClickListeners(isSubmitEnabled)
    }

    private fun initClickListeners(isSubmitEnabled: Boolean) {
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

    @SuppressLint("StringFormatInvalid")
    private fun initData() {
        viewModel.listServers()
        viewModel.serversList.observe(viewLifecycleOwner) { serversList ->
            servers = arrayListOf()
            servers.addAll(serversList)

            if (serversList.size > 1) {
                val listDropDown = mutableListOf<DropDownItem>()

                serversList.forEach { server ->
                    if (server is TellaReportServer) {
                        listDropDown.add(DropDownItem(server.projectId, server.projectName))
                    }
                }

                binding.dropdownGroup.show()
                binding.serversDropdown.setListAdapter(
                    listDropDown,
                    this@BaseReportsEntryFragment,
                    baseActivity
                )

                this@BaseReportsEntryFragment.reportInstance?.let {
                    servers.firstOrNull { server -> server.id == it.serverId }
                        ?.let { selectedServer ->
                            this.selectedServer = selectedServer
                            isServerSelected = true
                            binding.serversDropdown.setDefaultName(selectedServer.name)
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
        viewModel.reportInstance.observe(viewLifecycleOwner) { instance ->
            when (instance.status) {
                EntityStatus.DRAFT -> {
                    this@BaseReportsEntryFragment.reportInstance = instance
                    DialogUtils.showBottomMessage(
                        baseActivity, getString(R.string.Reports_Saved_Draft),
                        false
                    )
                }

                EntityStatus.SUBMISSION_PARTIAL_PARTS -> {
                    this@BaseReportsEntryFragment.submitReport(instance)
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
        viewModel.exitAfterSave.observe(viewLifecycleOwner) { exitAfterSave ->
            if (exitAfterSave) {
                nav().popBackStack()
            }
        }

        viewModel.instanceDeleted.observe(viewLifecycleOwner) {
            viewLifecycleOwner.lifecycleScope.launch {
                ReportsUtils.showReportDeletedSnackBar(
                    getString(
                        R.string.Report_Deleted_Confirmation, it
                    ), baseActivity
                )
                delay(200) // Delay for 200 milliseconds before popping the back stack
                nav().popBackStack() // Pop the back stack after showing the SnackBar
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
        servers[position].let {
            selectedServer = it
            isServerSelected = true
        }
        highLightButtons()
    }

    abstract fun submitReport(reportInstance: ReportInstance?)

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

    override fun onBackPressed(): Boolean {
        exitOrSave()
        return true
    }
}