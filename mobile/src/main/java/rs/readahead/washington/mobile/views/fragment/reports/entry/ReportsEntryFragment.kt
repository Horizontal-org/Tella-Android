package rs.readahead.washington.mobile.views.fragment.reports.entry

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.hzontal.tella_locking_ui.common.extensions.onChange
import com.hzontal.tella_vault.filter.FilterType
import dagger.hilt.android.AndroidEntryPoint
import org.hzontal.shared_ui.bottomsheet.VaultSheetUtils.IVaultFilesSelector
import org.hzontal.shared_ui.bottomsheet.VaultSheetUtils.showVaultSelectFilesSheet
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.databinding.FragmentReportsEntryBinding
import rs.readahead.washington.mobile.media.MediaFileHandler
import rs.readahead.washington.mobile.util.C
import rs.readahead.washington.mobile.util.hide
import rs.readahead.washington.mobile.util.setTint
import rs.readahead.washington.mobile.util.show
import rs.readahead.washington.mobile.views.activity.CameraActivity
import rs.readahead.washington.mobile.views.base_ui.BaseActivity
import rs.readahead.washington.mobile.views.base_ui.BaseBindingFragment
import rs.readahead.washington.mobile.views.fragment.uwazi.attachments.AttachmentsActivitySelector
import rs.readahead.washington.mobile.views.fragment.uwazi.attachments.VAULT_FILES_FILTER
import rs.readahead.washington.mobile.views.fragment.uwazi.attachments.VAULT_PICKER_SINGLE
import rs.readahead.washington.mobile.views.interfaces.ICollectEntryInterface

@AndroidEntryPoint
class ReportsEntryFragment :
    BaseBindingFragment<FragmentReportsEntryBinding>(FragmentReportsEntryBinding::inflate) {
    private val viewModel by viewModels<ReportsEntryViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        initView()
        initData()
    }

    private fun initView() {
        binding?.attachFilesBtn?.setOnClickListener {
            showSelectFilesSheet()
        }

        binding?.toolbar?.backClickListener = { nav().popBackStack() }

        highLightSubmitButton()
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
        } else {
            binding?.sendReportBtn?.setTint(R.color.wa_orange_16)
            binding?.sendReportBtn?.setOnClickListener(null)
        }
    }

    private fun saveReportAsDraft(){
      //  viewModel.saveDraft()
    }

    private fun initData() {
        viewModel.serversList.observe(viewLifecycleOwner, { serversList ->
            if (serversList.size > 1) {
                binding?.dropdownGroup?.show()
            } else {
                binding?.dropdownGroup?.hide()
            }
        })
    }

    private fun showSelectFilesSheet() {
        showVaultSelectFilesSheet(
            baseActivity.supportFragmentManager,
            baseActivity.getString(R.string.Uwazi_WidgetMedia_Take_Photo),
            baseActivity.getString(R.string.Vault_RecordAudio_SheetAction),
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
            val activity = context as Activity?
            activity!!.startActivityForResult(
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
        val activity = context as BaseActivity?
        activity!!.maybeChangeTemporaryTimeout {
            MediaFileHandler.startSelectMediaActivity(
                activity,
                "*/*",
                null,
                C.IMPORT_FILE
            )
            Unit
        }
    }

    private fun showAudioRecorderActivity() {
        try {
            val activity = context as ICollectEntryInterface?
            activity!!.openAudioRecorder()
        } catch (e: java.lang.Exception) {
            FirebaseCrashlytics.getInstance().recordException(e)
        }
    }


}