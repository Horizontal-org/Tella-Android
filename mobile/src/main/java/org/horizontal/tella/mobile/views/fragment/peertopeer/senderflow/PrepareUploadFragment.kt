package org.horizontal.tella.mobile.views.fragment.peertopeer.senderflow

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResultListener
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.hzontal.tella_locking_ui.common.extensions.onChange
import com.hzontal.tella_vault.VaultFile
import com.hzontal.tella_vault.filter.FilterType
import dagger.hilt.android.AndroidEntryPoint
import org.horizontal.tella.mobile.MyApplication
import org.horizontal.tella.mobile.R
import org.horizontal.tella.mobile.bus.EventObserver
import org.horizontal.tella.mobile.bus.event.AudioRecordEvent
import org.horizontal.tella.mobile.data.peertopeer.model.P2PFileStatus
import org.horizontal.tella.mobile.data.peertopeer.model.P2PSession
import org.horizontal.tella.mobile.data.peertopeer.model.ProgressFile
import org.horizontal.tella.mobile.databinding.FragmentPrepareUploadBinding
import org.horizontal.tella.mobile.domain.peertopeer.P2PFile
import org.horizontal.tella.mobile.media.MediaFileHandler
import org.horizontal.tella.mobile.util.C
import org.horizontal.tella.mobile.views.activity.camera.CameraActivity
import org.horizontal.tella.mobile.views.activity.camera.CameraActivity.Companion.CAPTURE_WITH_AUTO_UPLOAD
import org.horizontal.tella.mobile.views.adapters.reports.ReportsFilesRecyclerViewAdapter
import org.horizontal.tella.mobile.views.base_ui.BaseBindingFragment
import org.horizontal.tella.mobile.views.fragment.main_connexions.base.BUNDLE_REPORT_AUDIO
import org.horizontal.tella.mobile.views.fragment.main_connexions.base.BUNDLE_REPORT_VAULT_FILE
import org.horizontal.tella.mobile.views.fragment.main_connexions.base.OnNavBckListener
import org.horizontal.tella.mobile.views.fragment.peertopeer.viewmodel.FileTransferViewModel
import org.horizontal.tella.mobile.views.fragment.recorder.MicActivity
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

var PREPARE_UPLOAD_ENTRY = "PREPARE_UPLOAD_ENTRY"

@AndroidEntryPoint
class PrepareUploadFragment :
    BaseBindingFragment<FragmentPrepareUploadBinding>(FragmentPrepareUploadBinding::inflate),
    IReportAttachmentsHandler, OnNavBckListener {
    private lateinit var gridLayoutManager: GridLayoutManager
    private var isTitleEnabled = false
    private val viewModel: FileTransferViewModel by activityViewModels()
    private var disposables =
        MyApplication.bus().createCompositeDisposable()

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
        onAudioRecordingListener()
    }

    private fun initView() {
        binding.toolbar.backClickListener = {
            navManager().navigateBackToStartNearBySharingFragmentAndClearBackStack()
        }
        gridLayoutManager = GridLayoutManager(context, 3)
        binding.filesRecyclerView.apply {
            layoutManager = gridLayoutManager
            adapter = filesRecyclerViewAdapter
        }
        binding.toolbar.backClickListener = {
            exitOrSave()
        }

        //TODO HANDLE THIS IN THE NAVMANAGER
        val navBackStackEntry = findNavController().currentBackStackEntry
        navBackStackEntry?.savedStateHandle
            ?.getLiveData<Boolean>("transferRejected")
            ?.observe(viewLifecycleOwner) { wasRejected ->
                if (wasRejected) {
                    DialogUtils.showBottomMessage(
                        baseActivity, getString(R.string.recipient_rejected_the_files),
                        true
                    )
                }
            }
        findNavController().currentBackStackEntry
            ?.savedStateHandle
            ?.getLiveData<Boolean>("registrationSuccess")
            ?.observe(viewLifecycleOwner) { success ->
                if (success == true) {
                    DialogUtils.showBottomMessage(
                        baseActivity,
                        "Success! Connected to device",
                        false
                    )

                    // Remove it so it's not shown again on back navigation
                    findNavController().currentBackStackEntry?.savedStateHandle
                        ?.remove<Boolean>("registrationSuccess")
                }
            }
        binding.toolbar.backClickListener = {
            BottomSheetUtils.showConfirmSheet(
                baseActivity.supportFragmentManager,
                getString((R.string.exit_nearby_sharing)),
                getString(R.string.your_progress_will_be_lost),
                getString(R.string.action_exit),
                getString(R.string.action_cancel),
                object : BottomSheetUtils.ActionConfirmed {
                    override fun accept(isConfirmed: Boolean) {
                        if (isConfirmed) {
                            back()
                        }
                    }
                }
            )
        }
        highLightButtonsInit()
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
        navManager().navigateBackToStartNearBySharingFragmentAndClearBackStack()
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
        val intent = Intent(activity, MicActivity::class.java)
        intent.putExtra(PREPARE_UPLOAD_ENTRY, true)
        baseActivity.startActivity(intent)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == C.MEDIA_FILE_ID && resultCode == Activity.RESULT_OK) {
            val vaultFile = data?.getStringExtra(VAULT_FILE_KEY) ?: ""
            putFiles(viewModel.putVaultFilesInForm(vaultFile).blockingGet())
        }
    }

    private fun putFiles(vaultFileList: List<VaultFile>) {
        vaultFileList.forEach { file ->
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
            isTitleEnabled && filesRecyclerViewAdapter.getFiles()
                .isNotEmpty()

        val disabled: Float = context?.getString(R.string.alpha_disabled)?.toFloat() ?: 1.0f
        val enabled: Float = context?.getString(R.string.alpha_enabled)?.toFloat() ?: 1.0f

        binding.sendReportBtn.setBackgroundResource(if (isSubmitEnabled) R.drawable.bg_round_orange_btn else R.drawable.bg_round_orange16_btn)
        binding.sendReportBtn.alpha = (if (isSubmitEnabled) enabled else disabled)

        initClickListeners(isSubmitEnabled)
    }

    private fun initClickListeners(isSubmitEnabled: Boolean) {
        binding.sendReportBtn.setOnClickListener {
            if (isSubmitEnabled) {
                val selectedFiles = filesRecyclerViewAdapter.getFiles()
                viewModel.p2PSharedState.session?.title = binding.reportTitleEt.text.toString()

                if (selectedFiles.isNotEmpty()) {
                    val session = viewModel.p2PSharedState.session ?: P2PSession().also {
                        viewModel.p2PSharedState.session = it
                    }

                    selectedFiles.forEach { vaultFile ->
                        val p2pFile = P2PFile(
                            id = vaultFile.id,
                            fileName = vaultFile.name,
                            size = vaultFile.size,
                            fileType = vaultFile.mimeType ?: "application/octet-stream",
                            sha256 = vaultFile.hash,
                            thumb = vaultFile.thumb
                        )

                        val progressFile = ProgressFile(
                            file = p2pFile,
                            vaultFile = vaultFile,
                            status = P2PFileStatus.QUEUE
                        )

                        session.files[vaultFile.id] = progressFile
                    }

                    // Navigate
                    navManager().navigateFromPrepareUploadFragmentToWaitingSenderFragment()
                } else {
                    showToast("No file selected")
                }
            } else {
                showSubmitReportErrorSnackBar()
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

    override fun playMedia(mediaFile: VaultFile?) {
    }

    override fun addFiles() {
        showSelectFilesSheet()
    }

    override fun onBackPressed(): Boolean {
        exitOrSave()
        return true
    }

    private fun onAudioRecordingListener() {
        disposables.wire(
            AudioRecordEvent::class.java,
            object : EventObserver<AudioRecordEvent?>() {
                override fun onNext(event: AudioRecordEvent) {
                    putFiles(listOf(event.vaultFile))
                }
            })
    }

}