package org.horizontal.tella.mobile.views.fragment.peertopeer.senderflow

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Toast
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.hzontal.tella_locking_ui.common.extensions.onChange
import com.hzontal.tella_vault.VaultFile
import com.hzontal.tella_vault.filter.FilterType
import dagger.hilt.android.AndroidEntryPoint
import org.horizontal.tella.mobile.R
import org.horizontal.tella.mobile.databinding.FragmentPrepareUploadBinding
import org.horizontal.tella.mobile.domain.entity.reports.ReportInstance
import org.horizontal.tella.mobile.media.MediaFileHandler
import org.horizontal.tella.mobile.util.C
import org.horizontal.tella.mobile.views.activity.camera.CameraActivity
import org.horizontal.tella.mobile.views.activity.camera.CameraActivity.Companion.CAPTURE_WITH_AUTO_UPLOAD
import org.horizontal.tella.mobile.views.activity.viewer.sharedViewModel
import org.horizontal.tella.mobile.views.adapters.reports.ReportsFilesRecyclerViewAdapter
import org.horizontal.tella.mobile.views.base_ui.BaseBindingFragment
import org.horizontal.tella.mobile.views.fragment.main_connexions.base.BUNDLE_REPORT_AUDIO
import org.horizontal.tella.mobile.views.fragment.main_connexions.base.BUNDLE_REPORT_FORM_INSTANCE
import org.horizontal.tella.mobile.views.fragment.main_connexions.base.BUNDLE_REPORT_VAULT_FILE
import org.horizontal.tella.mobile.views.fragment.main_connexions.base.OnNavBckListener
import org.horizontal.tella.mobile.views.fragment.peertopeer.SenderViewModel
import org.horizontal.tella.mobile.views.fragment.recorder.REPORT_ENTRY
import org.horizontal.tella.mobile.views.fragment.uwazi.attachments.AttachmentsActivitySelector
import org.horizontal.tella.mobile.views.fragment.uwazi.attachments.VAULT_FILES_FILTER
import org.horizontal.tella.mobile.views.fragment.uwazi.attachments.VAULT_FILE_KEY
import org.horizontal.tella.mobile.views.fragment.uwazi.attachments.VAULT_PICKER_SINGLE
import org.horizontal.tella.mobile.views.interfaces.IReportAttachmentsHandler
import org.hzontal.shared_ui.bottomsheet.BottomSheetUtils
import org.hzontal.shared_ui.bottomsheet.VaultSheetUtils.IVaultFilesSelector
import org.hzontal.shared_ui.bottomsheet.VaultSheetUtils.showVaultSelectFilesSheet
import org.hzontal.shared_ui.utils.DialogUtils

@AndroidEntryPoint
class PrepareUploadFragment :
    BaseBindingFragment<FragmentPrepareUploadBinding>(FragmentPrepareUploadBinding::inflate),
    IReportAttachmentsHandler, OnNavBckListener {
    private lateinit var gridLayoutManager: GridLayoutManager
    private var isTitleEnabled = false
    private var reportInstance: ReportInstance? = null
    private val viewModel: SenderViewModel by viewModels()
    private var isNewDraft = true

    private val filesRecyclerViewAdapter: ReportsFilesRecyclerViewAdapter by lazy {
        ReportsFilesRecyclerViewAdapter(this)
    }

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
            putFiles(viewModel.mediaFilesToVaultFiles(instance.widgetMediaFiles))
            isNewDraft = false
        }
        parentFragmentManager.setFragmentResultListener("prepare_upload_result", viewLifecycleOwner) { _, result ->
            val wasRejected = result.getBoolean("rejected", false)
            if (wasRejected) {
                showTooltip(
                    binding.root,
                    "Recipient rejected the files.",
                    Gravity.TOP
                )
            }
        }
        viewModel.prepareResults.observe(viewLifecycleOwner) { response ->
                val id = response.transmissionId
                // navigate to next screen
        }

        binding.toolbar.backClickListener = {  BottomSheetUtils.showConfirmSheet(
            baseActivity.supportFragmentManager,
            getString((R.string.exit_nearby_sharing)),
            getString(R.string.your_progress_will_be_lost),
            getString(R.string.action_exit),
            getString(R.string.action_cancel),
            object : BottomSheetUtils.ActionConfirmed {
                override fun accept(isConfirmed: Boolean) {
                    if (isConfirmed) {
                        findNavController().popBackStack()
                    }
                }
            }
        )
        }
        highLightButtonsInit()
        checkIsNewDraftEntry()
    }

    private fun checkIsNewDraftEntry() {
        if (isNewDraft) {
           // binding.deleteBtn.invisible()
          //  binding.sendLaterBtn.show()
            binding.sendReportBtn.text = getString(R.string.collect_end_action_submit)
        } else {
           // binding.deleteBtn.show()
          //  binding.sendLaterBtn.invisible()
            binding.sendReportBtn.text = getString(R.string.Send_Action_Label)
        }
    }



    private fun highLightButtonsInit() {
        binding.apply {
            reportTitleEt.let { title ->
                isTitleEnabled = title.length() > 0
            }

            reportTitleEt.onChange { title ->
                isTitleEnabled = title.isNotEmpty()
                highLightButtons()
            }

        }

    }

    private fun exitOrSave() {

    }

    @SuppressLint("StringFormatInvalid")
    private fun initData() {


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
            putFiles(viewModel.putVaultFilesInForm(vaultFile).blockingGet())
        }
    }

    private fun putFiles(vaultFileList: List<VaultFile>) {
        val filteredFiles = vaultFileList.filter { file ->
            isValidFile(file)
        }.also {
            if (it.size != vaultFileList.size) {
                showToast(getString(R.string.nextcloud_file_size_limit))
            }
        }

        // Insert the filtered files
        filteredFiles.forEach { file ->
            filesRecyclerViewAdapter.insertAttachment(file)
        }

        // Ensure visibility and highlight buttons
        binding.filesRecyclerView.visibility = View.VISIBLE
        highLightButtons()
    }

    override fun removeFiles() {
        highLightButtons()
    }

    private fun highLightButtons() {
        val isSubmitEnabled =
            isTitleEnabled  && filesRecyclerViewAdapter.getFiles()
                .isNotEmpty()

        val disabled: Float = context?.getString(R.string.alpha_disabled)?.toFloat() ?: 1.0f
        val enabled: Float = context?.getString(R.string.alpha_enabled)?.toFloat() ?: 1.0f

        binding.sendReportBtn.setBackgroundResource(if (isSubmitEnabled) R.drawable.bg_round_orange_btn else R.drawable.bg_round_orange16_btn)
        binding.sendReportBtn.alpha = (if (isSubmitEnabled) enabled else disabled)

        initClickListeners(isSubmitEnabled)
    }

    private fun initClickListeners(isSubmitEnabled: Boolean) {
        binding.sendReportBtn.setOnClickListener {
            binding.sendReportBtn.setOnClickListener {
                if (isSubmitEnabled) {
                    val selectedFiles = filesRecyclerViewAdapter.getFiles()
                    if (selectedFiles.isNotEmpty()) {
                        viewModel.prepareUploadsFromVaultFiles(selectedFiles)
                        // navigate to waiting view
                        bundle.putBoolean("isSender", true)
                        navManager().navigateFromPrepareUploadFragmentToWaitingFragment()
                    } else {
                        showToast("No file selected")
                    }
                } else {
                    showSubmitReportErrorSnackBar()
                }
            }
        }
    }
    private fun showSubmitReportErrorSnackBar() {
        val errorRes = R.string.Snackbar_Submit_Files_Error

        DialogUtils.showBottomMessage(
            baseActivity,
            getString(errorRes),
            false
        )
    }

    // Helper function to check if the file is valid (less than or equal to 20MB)
    private fun isValidFile(file: VaultFile): Boolean {
        val isFileSizeValid = file.size <= 20 * 1024 * 1024 // 20MB in bytes
        return isFileSizeValid
    }

    override fun playMedia(mediaFile: VaultFile?) {
    }

    override fun addFiles() {
        showSelectFilesSheet()
    }

    override fun onBackPressed(): Boolean {
        exitOrSave()
        return true
    }

}