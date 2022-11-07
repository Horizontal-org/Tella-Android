package rs.readahead.washington.mobile.views.fragment.reports.entry

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.hzontal.tella_locking_ui.common.extensions.onChange
import com.hzontal.tella_vault.VaultFile
import com.hzontal.tella_vault.filter.FilterType
import dagger.hilt.android.AndroidEntryPoint
import org.hzontal.shared_ui.bottomsheet.VaultSheetUtils.IVaultFilesSelector
import org.hzontal.shared_ui.bottomsheet.VaultSheetUtils.showVaultSelectFilesSheet
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.databinding.FragmentReportsEntryBinding
import rs.readahead.washington.mobile.domain.entity.reports.ReportFormInstance
import rs.readahead.washington.mobile.domain.entity.reports.TellaReportServer
import rs.readahead.washington.mobile.media.MediaFileHandler
import rs.readahead.washington.mobile.util.C
import rs.readahead.washington.mobile.util.hide
import rs.readahead.washington.mobile.util.setTint
import rs.readahead.washington.mobile.util.show
import rs.readahead.washington.mobile.views.activity.CameraActivity
import rs.readahead.washington.mobile.views.adapters.reports.ReportsFilesRecyclerViewAdapter
import rs.readahead.washington.mobile.views.base_ui.BaseBindingFragment
import rs.readahead.washington.mobile.views.fragment.uwazi.attachments.AttachmentsActivitySelector
import rs.readahead.washington.mobile.views.fragment.uwazi.attachments.VAULT_FILES_FILTER
import rs.readahead.washington.mobile.views.fragment.uwazi.attachments.VAULT_FILE_KEY
import rs.readahead.washington.mobile.views.fragment.uwazi.attachments.VAULT_PICKER_SINGLE
import rs.readahead.washington.mobile.views.interfaces.IReportAttachmentsHandler

const val BUNDLE_REPORT_FORM_INSTANCE = "bundle_report_form_instance"

@AndroidEntryPoint
class ReportsEntryFragment :
    BaseBindingFragment<FragmentReportsEntryBinding>(FragmentReportsEntryBinding::inflate),
    IReportAttachmentsHandler {
    private val viewModel by viewModels<ReportsEntryViewModel>()
    private lateinit var gridLayoutManager: GridLayoutManager
    private val filesRecyclerViewAdapter: ReportsFilesRecyclerViewAdapter by lazy {
        ReportsFilesRecyclerViewAdapter(
            this,
            baseActivity, MediaFileHandler()
        )
    }
    private lateinit var selectedServer: TellaReportServer
    private var reportFormInstance: ReportFormInstance? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        initView()
        initData()
    }

    private fun initView() {
        gridLayoutManager = GridLayoutManager(context, 3)

        binding?.attachFilesBtn?.setOnClickListener {
            showSelectFilesSheet()
        }
        binding?.filesRecyclerView?.apply {
            adapter = filesRecyclerViewAdapter
            layoutManager = gridLayoutManager
        }

        binding?.toolbar?.backClickListener = { nav().popBackStack() }

        highLightSubmitButton()

        arguments?.let { bundle ->
            reportFormInstance = bundle.get(BUNDLE_REPORT_FORM_INSTANCE) as ReportFormInstance
            putFiles(viewModel.mediaFilesToVaultFiles(reportFormInstance!!.widgetMediaFiles))
        }

        reportFormInstance?.let { instance ->
            binding?.reportTitleEt?.setText(instance.title)
            binding?.reportDescriptionEt?.setText(instance.description)
        }

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
            binding?.sendReportBtn?.setTint(R.color.wa_orange)
            binding?.sendReportBtn?.setOnClickListener {}
            binding?.sendLaterBtn?.setOnClickListener {}
            binding?.toolbar?.onRightClickListener = {
                saveReportAsDraft()
            }
            binding?.sendLaterBtn?.setOnClickListener {
                saveReportAsOutbox()
            }

        } else {
            binding?.sendReportBtn?.setTint(R.color.wa_orange_16)
            binding?.sendReportBtn?.setOnClickListener(null)
            binding?.toolbar?.onRightClickListener = {}
            binding?.sendLaterBtn?.setOnClickListener {}
        }
    }

    private fun saveReportAsDraft() {
        viewModel.saveDraft(
            viewModel.getDraftFormInstance(
                binding?.reportTitleEt?.text.toString(),
                binding?.reportDescriptionEt?.text.toString(),
                files = viewModel.vaultFilesToMediaFiles(filesRecyclerViewAdapter.getFiles()),
                server = selectedServer
            )
        )
    }

    private fun saveReportAsOutbox() {
        viewModel.saveOutbox(
            viewModel.getOutboxFormInstance(
                binding?.reportTitleEt?.text.toString(),
                binding?.reportDescriptionEt?.text.toString(),
                files = viewModel.vaultFilesToMediaFiles(filesRecyclerViewAdapter.getFiles()),
                server = selectedServer
            )
        )
    }

    private fun initData() {
        with(viewModel) {
            listServers()
            serversList.observe(viewLifecycleOwner, { serversList ->
                if (serversList.size > 1) {
                    binding?.dropdownGroup?.show()
                } else {
                    binding?.dropdownGroup?.hide()
                    selectedServer = serversList[0]
                }
            })

            draftReportFormInstance.observe(viewLifecycleOwner, {
                nav().popBackStack()
            })

            outboxReportFormInstance.observe(viewLifecycleOwner, {
                nav().popBackStack()
            })
        }

    }

    private fun showSelectFilesSheet() {
        showVaultSelectFilesSheet(
            baseActivity.supportFragmentManager,
            baseActivity.getString(R.string.Uwazi_WidgetMedia_Take_Photo),
            null,//baseActivity.getString(R.string.Vault_RecordAudio_SheetAction),
            baseActivity.getString(R.string.Uwazi_WidgetMedia_Select_From_Device),
            baseActivity.getString(R.string.Uwazi_WidgetMedia_Select_From_Tella),
            null,
            baseActivity.getString(R.string.Uwazi_MiltiFileWidget_SelectFiles),
            object : IVaultFilesSelector {
                override fun importFromVault() {
                    showAttachmentsFragment()
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
            }
        )
    }

    private fun showAttachmentsFragment() {
        try {
            //TODO Djordje CONSIDER USING PERMISSION LIBRARY INSTEAD
            baseActivity.startActivityForResult(
                Intent(activity, AttachmentsActivitySelector::class.java)
                    // .putExtra(VAULT_FILE_KEY, Gson().toJson(ids))
                    .putExtra(
                        VAULT_FILES_FILTER,
                        FilterType.ALL_WITHOUT_DIRECTORY
                    )
                    .putExtra(VAULT_PICKER_SINGLE, false),
                C.MEDIA_FILE_ID
            )
        } catch (e: Exception) {
            FirebaseCrashlytics.getInstance().recordException(e)
        }
    }

    private fun showCameraActivity() {
        try {
            //TODO Djordje WE SHOULD BE ABLE TO USE `baseActivity instance` instead
            baseActivity.startActivityForResult(
                Intent(context, CameraActivity::class.java)
                    .putExtra(
                        CameraActivity.INTENT_MODE,
                        CameraActivity.IntentMode.COLLECT.name
                    ),
                C.MEDIA_FILE_ID
            )
        } catch (e: Exception) {
            FirebaseCrashlytics.getInstance().recordException(e)
        }
    }

    private fun importMedia() {
        baseActivity.maybeChangeTemporaryTimeout {
            MediaFileHandler.startSelectMediaActivity(
                activity,
                "*/*",
                null,
                C.IMPORT_FILE
            )
        }
    }

    private fun showAudioRecorderActivity() {
        /*try {
            bundle.putString(COLLECT_ENTRY, true.toString())
            nav().navigate(R.id.action_newReport_to_micScreen, bundle)

        } catch (e: java.lang.Exception) {
            FirebaseCrashlytics.getInstance().recordException(e)
        }*/
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == C.MEDIA_FILE_ID && resultCode == Activity.RESULT_OK) {
            val vaultFile = data?.getStringExtra(VAULT_FILE_KEY) ?: ""
            putFiles(viewModel.putVaultFilesInForm(vaultFile))
        }
    }

    private fun putFiles(vaultFileList: ArrayList<VaultFile>) {
        for (file in vaultFileList) {
            filesRecyclerViewAdapter.insertAttachment(file)
        }
        binding?.filesRecyclerView?.visibility = View.VISIBLE
    }

    override fun playMedia(mediaFile: VaultFile?) {}

    override fun onRemovedAttachments() {
        binding?.filesRecyclerView?.visibility = View.GONE
    }
}