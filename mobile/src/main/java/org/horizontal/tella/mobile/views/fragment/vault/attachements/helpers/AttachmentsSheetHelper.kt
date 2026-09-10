package org.horizontal.tella.mobile.views.fragment.vault.attachements.helpers

import android.content.Context
import androidx.fragment.app.FragmentManager
import com.hzontal.tella_vault.VaultFile
import org.hzontal.shared_ui.bottomsheet.BottomSheetUtils
import org.horizontal.tella.mobile.R
import org.horizontal.tella.mobile.media.MediaFileHandler
import org.horizontal.tella.mobile.views.base_ui.BaseActivity

object AttachmentsSheetHelper {

    private const val OPTION_MEDIA_AND_VERIFICATION = 1
    private const val OPTION_ONLY_MEDIA = 0

    /**
     * Create and return options map for share/export dialogs that may include verification metadata.
     */
    fun getShareDialogOptions(): LinkedHashMap<Int, Int> {
        return linkedMapOf(
            OPTION_MEDIA_AND_VERIFICATION to R.string.verification_share_select_media_and_verification,
            OPTION_ONLY_MEDIA to R.string.verification_share_select_only_media
        )
    }

    fun showIncludeVerificationDialog(
        fragmentManager: FragmentManager,
        context: Context,
        onResult: (includeMetadata: Boolean) -> Unit
    ) {
        BottomSheetUtils.showRadioListOptionsSheet(
            fragmentManager,
            context,
            getShareDialogOptions(),
            context.getString(R.string.verification_share_dialog_title),
            context.getString(R.string.verification_share_dialog_expl),
            context.getString(R.string.action_ok),
            context.getString(R.string.action_cancel),
            object : BottomSheetUtils.RadioOptionConsumer {
                override fun accept(option: Int) {
                    onResult(option == OPTION_MEDIA_AND_VERIFICATION)
                }
            }
        )
    }

    /**
     * Show a dialog to share multiple files with metadata
     */
    internal fun showShareWithMetadataDialog(activity: BaseActivity, selected: List<VaultFile>) {
        showIncludeVerificationDialog(
            activity.supportFragmentManager,
            activity
        ) { includeMetadata ->
            AttachmentsHelper.startShareActivity(includeMetadata, selected, activity)
        }
    }

    /**
     * Show a dialog to share a single file with metadata
     */
    internal fun showShareFileWithMetadataDialog(vaultFile: VaultFile, activity: BaseActivity) {
        showIncludeVerificationDialog(
            activity.supportFragmentManager,
            activity
        ) { includeMetadata ->
            MediaFileHandler.startShareActivity(activity, vaultFile, includeMetadata)
        }
    }

}
