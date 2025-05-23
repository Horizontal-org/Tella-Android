package org.horizontal.tella.mobile.views.fragment.peertopeer.senderflow

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.fragment.app.setFragmentResultListener
import androidx.recyclerview.widget.GridLayoutManager
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.hzontal.tella_vault.VaultFile
import com.hzontal.tella_vault.filter.FilterType
import dagger.hilt.android.AndroidEntryPoint
import org.horizontal.tella.mobile.R
import org.horizontal.tella.mobile.databinding.FragmentPrepareUploadBinding
import org.horizontal.tella.mobile.media.MediaFileHandler
import org.horizontal.tella.mobile.util.C
import org.horizontal.tella.mobile.views.activity.camera.CameraActivity
import org.horizontal.tella.mobile.views.activity.camera.CameraActivity.Companion.CAPTURE_WITH_AUTO_UPLOAD
import org.horizontal.tella.mobile.views.adapters.reports.ReportsFilesRecyclerViewAdapter
import org.horizontal.tella.mobile.views.base_ui.BaseBindingFragment
import org.horizontal.tella.mobile.views.fragment.main_connexions.base.BUNDLE_REPORT_AUDIO
import org.horizontal.tella.mobile.views.fragment.main_connexions.base.BUNDLE_REPORT_VAULT_FILE
import org.horizontal.tella.mobile.views.fragment.main_connexions.base.OnNavBckListener
import org.horizontal.tella.mobile.views.fragment.recorder.REPORT_ENTRY
import org.horizontal.tella.mobile.views.fragment.uwazi.attachments.AttachmentsActivitySelector
import org.horizontal.tella.mobile.views.fragment.uwazi.attachments.VAULT_FILES_FILTER
import org.horizontal.tella.mobile.views.fragment.uwazi.attachments.VAULT_FILE_KEY
import org.horizontal.tella.mobile.views.fragment.uwazi.attachments.VAULT_PICKER_SINGLE
import org.horizontal.tella.mobile.views.interfaces.IReportAttachmentsHandler
import org.hzontal.shared_ui.bottomsheet.VaultSheetUtils.IVaultFilesSelector
import org.hzontal.shared_ui.bottomsheet.VaultSheetUtils.showVaultSelectFilesSheet

@AndroidEntryPoint
class PrepareUploadFragment :
    BaseBindingFragment<FragmentPrepareUploadBinding>(FragmentPrepareUploadBinding::inflate),
    IReportAttachmentsHandler, OnNavBckListener {
    private lateinit var gridLayoutManager: GridLayoutManager
    private val filesRecyclerViewAdapter: ReportsFilesRecyclerViewAdapter by lazy {
        ReportsFilesRecyclerViewAdapter(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setFragmentResultListener(BUNDLE_REPORT_AUDIO) { _, bundle ->
            val file = bundle.get(BUNDLE_REPORT_VAULT_FILE) as VaultFile
            bundle.remove(BUNDLE_REPORT_VAULT_FILE)
            //putFiles(listOf(file))
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
         //   putFiles(viewModel.putVaultFilesInForm(vaultFile).blockingGet())
        }
    }


    override fun playMedia(mediaFile: VaultFile?) {

    }

    override fun addFiles() {
        showSelectFilesSheet()
    }

    override fun removeFiles() {
    }

    override fun onBackPressed(): Boolean {
        exitOrSave()
        return true
    }

}