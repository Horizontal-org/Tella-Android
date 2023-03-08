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
import rs.readahead.washington.mobile.domain.entity.reports.ReportFormInstance
import rs.readahead.washington.mobile.domain.entity.reports.TellaReportServer
import rs.readahead.washington.mobile.media.MediaFileHandler
import rs.readahead.washington.mobile.util.C
import rs.readahead.washington.mobile.util.hide
import rs.readahead.washington.mobile.util.show
import rs.readahead.washington.mobile.views.activity.CameraActivity
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
    private var servers: ArrayList<TellaReportServer>? = null
    private var reportFormInstance: ReportFormInstance? = null

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
        binding?.filesRecyclerView?.apply {
            layoutManager = gridLayoutManager
            adapter = filesRecyclerViewAdapter
        }
        binding?.toolbar?.backClickListener = {
            exitOrSave()
        }
        highLightSubmitButton()
        arguments?.let { bundle ->
            if (bundle.get(BUNDLE_REPORT_FORM_INSTANCE) != null) {
                reportFormInstance = bundle.get(BUNDLE_REPORT_FORM_INSTANCE) as ReportFormInstance
                bundle.remove(BUNDLE_REPORT_FORM_INSTANCE)
            }
        }

        reportFormInstance?.let { instance ->
            binding?.reportTitleEt?.setText(instance.title)
            binding?.reportDescriptionEt?.setText(instance.description)
            putFiles(viewModel.mediaFilesToVaultFiles(instance.widgetMediaFiles))
        }
    }

    private fun exitOrSave() {
        val title = binding?.reportTitleEt!!.text.toString()
        val description = binding?.reportDescriptionEt!!.text.toString()
        if (reportFormInstance == null && title.isEmpty()) {
            nav().popBackStack()
        } else if (reportFormInstance == null && title.isNotEmpty()) {
            showConfirmSaveOrExit(reportFormInstance)
        } else if (reportFormInstance != null && (reportFormInstance?.title != title || reportFormInstance?.description != description
                    || filesRecyclerViewAdapter.getFiles() != viewModel.mediaFilesToVaultFiles(
                reportFormInstance?.widgetMediaFiles
            ))
        ) {
            showConfirmSaveOrExit(reportFormInstance)
        } else {
            nav().popBackStack()
        }
    }

    private fun showConfirmSaveOrExit(reportFormInstance: ReportFormInstance?) {
        BottomSheetUtils.showConfirmSheet(
            baseActivity.supportFragmentManager,
            getString(R.string.Report_Dialog_Draft_Title),
            getString(R.string.collect_form_exit_dialog_expl),
            getString(R.string.collect_form_exit_dialog_action_save_exit),
            getString(R.string.collect_form_exit_dialog_action_exit_anyway),
            object : BottomSheetUtils.ActionConfirmed {
                override fun accept(isConfirmed: Boolean) {
                    if (isConfirmed) {
                        if (reportFormInstance == null) {
                            saveReportAsDraft(true)
                        } else {
                            if (reportFormInstance.status == EntityStatus.DRAFT) {
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

    private fun highLightSubmitButton() {
        var isTitleEnabled = false
        var isDescriptionEnabled = false
        binding?.reportTitleEt?.onChange { title ->
            isTitleEnabled = title.length > 1
            highLightButton(isTitleEnabled, isDescriptionEnabled)
        }
        binding?.reportDescriptionEt?.onChange { description ->
            isDescriptionEnabled = description.length > 1
            highLightButton(isTitleEnabled, isDescriptionEnabled)
        }

    }

    private fun highLightButton(isTitleEnabled: Boolean, isDescriptionEnabled: Boolean) {
        if (isTitleEnabled && isDescriptionEnabled) {
            binding?.sendReportBtn?.setBackgroundResource(R.drawable.bg_round_orange_btn)
            binding?.toolbar?.onRightClickListener = {
                saveReportAsDraft(false)
            }
            binding?.sendLaterBtn?.setOnClickListener {
                saveReportAsOutbox()
            }
            binding?.sendReportBtn?.setOnClickListener {
                saveReportAsPending()
            }
        } else {
            binding?.sendReportBtn?.setBackgroundResource(R.drawable.bg_round_orange16_btn)
            binding?.sendReportBtn?.setOnClickListener(null)
            binding?.toolbar?.onRightClickListener = {}
            binding?.sendLaterBtn?.setOnClickListener {}
        }
    }

    private fun saveReportAsDraft(exitAfterSave: Boolean) {
        viewModel.saveDraft(
            viewModel.getDraftFormInstance(
                id = reportFormInstance?.id,
                title = binding?.reportTitleEt?.text.toString(),
                description = binding?.reportDescriptionEt?.text.toString(),
                files = viewModel.vaultFilesToMediaFiles(filesRecyclerViewAdapter.getFiles()),
                server = selectedServer
            ),
            exitAfterSave
        )
    }

    private fun saveReportAsOutbox() {
        viewModel.saveOutbox(
            viewModel.getFormInstance(
                id = reportFormInstance?.id,
                title = binding?.reportTitleEt?.text.toString(),
                description = binding?.reportDescriptionEt?.text.toString(),
                files = viewModel.vaultFilesToMediaFiles(filesRecyclerViewAdapter.getFiles()),
                server = selectedServer,
                status = EntityStatus.FINALIZED
            )
        )
    }

    private fun saveReportAsPending() {
        viewModel.saveOutbox(
            viewModel.getFormInstance(
                id = reportFormInstance?.id,
                title = binding?.reportTitleEt?.text.toString(),
                description = binding?.reportDescriptionEt?.text.toString(),
                files = viewModel.vaultFilesToMediaFiles(filesRecyclerViewAdapter.getFiles()),
                server = selectedServer,
                status = EntityStatus.SUBMISSION_PENDING
            )
        )
    }

    private fun initData() {
        with(viewModel) {
            listServers()
            serversList.observe(viewLifecycleOwner) { serversList ->
                if (serversList.size > 1) {
                    servers = arrayListOf()
                    servers?.addAll(serversList)
                    val listDropDown = mutableListOf<DropDownItem>()
                    serversList.map { server ->
                        listDropDown.add(DropDownItem(server.projectId, server.projectName))
                    }
                    binding?.dropdownGroup?.show()
                    binding?.serversDropdown?.setListAdapter(
                        listDropDown,
                        this@ReportsEntryFragment,
                        baseActivity
                    )
                    reportFormInstance?.let {
                        servers?.first { server -> server.id == it.serverId }?.let {
                            selectedServer = it
                            binding?.serversDropdown?.setDefaultName(it.name)
                        }
                    }
                } else {
                    binding?.dropdownGroup?.hide()
                    selectedServer = serversList[0]
                }
            }
            reportInstance.observe(viewLifecycleOwner) { instance ->
                when (instance.status) {
                    EntityStatus.DRAFT -> {
                        reportFormInstance = instance
                        DialogUtils.showBottomMessage(
                            baseActivity, getString(R.string.Reports_Saved_Draft),
                            false
                        )
                    }
                    EntityStatus.SUBMISSION_PENDING -> {
                        submitReport()
                    }
                    EntityStatus.FINALIZED -> {
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
            //TODO Djordje WE SHOULD BE ABLE TO USE `baseActivity instance` instead
            baseActivity.startActivityForResult(
                Intent(context, CameraActivity::class.java).putExtra(
                    CameraActivity.INTENT_MODE, CameraActivity.IntentMode.COLLECT.name
                ), C.MEDIA_FILE_ID
            )
        } catch (e: Exception) {
            FirebaseCrashlytics.getInstance().recordException(e)
        }
    }

    private fun importMedia() {
        baseActivity.maybeChangeTemporaryTimeout {
            MediaFileHandler.startSelectMediaActivity(
                activity, "*/*", null, C.IMPORT_FILE
            )
        }
    }

    private fun showAudioRecorderActivity() {
        try {
            val bundle = Bundle()
            bundle.putBoolean(REPORT_ENTRY, true)
            nav().navigate(R.id.action_newReport_to_micScreen, bundle)
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
        binding?.filesRecyclerView?.visibility = View.VISIBLE
    }

    override fun playMedia(mediaFile: VaultFile?) {

    }

    override fun addFiles() {
        showSelectFilesSheet()
    }

    override fun onDropDownItemClicked(position: Int, chosenItem: DropDownItem) {
        binding?.serversDropdown?.setDefaultName(chosenItem.name)
        servers?.get(position)?.let {
            selectedServer = it
        }
    }

    private fun submitReport() {
        val bundle = Bundle()
        reportFormInstance = viewModel.getFinalizedFormInstance(
            id = reportFormInstance?.id,
            title = binding?.reportTitleEt?.text.toString(),
            description = binding?.reportDescriptionEt?.text.toString(),
            files = viewModel.vaultFilesToMediaFiles(filesRecyclerViewAdapter.getFiles()),
            server = selectedServer
        )

        bundle.putSerializable(BUNDLE_REPORT_FORM_INSTANCE, reportFormInstance)
        // nav().navigateUp()
        nav().navigate(R.id.action_newReport_to_reportSendScreen, bundle)
        nav().clearBackStack(R.id.action_newReport_to_reportSendScreen)
    }
}