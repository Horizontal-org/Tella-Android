package rs.readahead.washington.mobile.views.fragment.vault.attachements.helpers

import com.hzontal.tella_vault.VaultFile
import org.hzontal.shared_ui.bottomsheet.BottomSheetUtils
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.media.MediaFileHandler
import rs.readahead.washington.mobile.views.base_ui.BaseActivity

object AttachmentsSheetHelper {

    private const val OPTION_MEDIA_AND_VERIFICATION = 1
    private const val OPTION_ONLY_MEDIA = 0

    /**
     * Create and return options map for share dialog.
     */
    private fun getShareDialogOptions(): LinkedHashMap<Int, Int> {
        return linkedMapOf(
            OPTION_MEDIA_AND_VERIFICATION to R.string.verification_share_select_media_and_verification,
            OPTION_ONLY_MEDIA to R.string.verification_share_select_only_media
        )
    }

    /**
     * Show a dialog to share multiple files with metadata
     */
    internal fun showShareWithMetadataDialog(activity: BaseActivity, selected: List<VaultFile>) {
        val options = getShareDialogOptions()
        BottomSheetUtils.showRadioListOptionsSheet(
            activity.supportFragmentManager,
            activity,
            options,
            activity.getString(R.string.verification_share_dialog_title),
            activity.getString(R.string.verification_share_dialog_expl),
            activity.getString(R.string.action_ok),
            activity.getString(R.string.action_cancel),
            object : BottomSheetUtils.RadioOptionConsumer {
                override fun accept(option: Int) {
                    AttachmentsHelper.startShareActivity(
                        option == OPTION_MEDIA_AND_VERIFICATION,
                        selected,
                        activity
                    )
                }
            }
        )
    }

    /**
     * Show a dialog to share a single file with metadata
     */
    internal fun showShareFileWithMetadataDialog(vaultFile: VaultFile, activity: BaseActivity) {
        val options = getShareDialogOptions()
        BottomSheetUtils.showRadioListOptionsSheet(
            activity.supportFragmentManager,
            activity,
            options,
            activity.getString(R.string.verification_share_dialog_title),
            activity.getString(R.string.verification_share_dialog_expl),
            activity.getString(R.string.action_ok),
            activity.getString(R.string.action_cancel),
            object : BottomSheetUtils.RadioOptionConsumer {
                override fun accept(option: Int) {
                    MediaFileHandler.startShareActivity(
                        activity,
                        vaultFile,
                        option == OPTION_MEDIA_AND_VERIFICATION
                    )
                }
            }
        )
    }

}