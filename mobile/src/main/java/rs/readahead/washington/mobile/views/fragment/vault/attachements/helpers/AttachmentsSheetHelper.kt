package rs.readahead.washington.mobile.views.fragment.vault.attachements.helpers

import com.hzontal.tella_vault.VaultFile
import org.hzontal.shared_ui.bottomsheet.BottomSheetUtils
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.media.MediaFileHandler
import rs.readahead.washington.mobile.views.base_ui.BaseActivity
import rs.readahead.washington.mobile.views.fragment.vault.attachements.helpers.AttachmentsHelper

object AttachmentsSheetHelper {

    internal fun showShareWithMetadataDialog(activity: BaseActivity, selected : List<VaultFile>) {
        val options = LinkedHashMap<Int, Int>()
        options[1] = R.string.verification_share_select_media_and_verification
        options[0] = R.string.verification_share_select_only_media
        BottomSheetUtils.showRadioListOptionsSheet(activity.supportFragmentManager,
            activity,
            options,
            activity.getString(R.string.verification_share_dialog_title),
            activity.getString(R.string.verification_share_dialog_expl),
            activity.getString(R.string.action_ok),
            activity.getString(R.string.action_cancel),
            object : BottomSheetUtils.RadioOptionConsumer {
                override fun accept(option: Int) {
                    AttachmentsHelper.startShareActivity(option > 0,selected,activity)
                }
            })
    }

    internal fun showShareFileWithMetadataDialog(vaultFile: VaultFile, activity: BaseActivity) {
        val options = LinkedHashMap<Int, Int>()
        options[1] = R.string.verification_share_select_media_and_verification
        options[0] = R.string.verification_share_select_only_media
        BottomSheetUtils.showRadioListOptionsSheet(activity.supportFragmentManager,
            activity,
            options,
            activity.getString(R.string.verification_share_dialog_title),
            activity.getString(R.string.verification_share_dialog_expl),
            activity.getString(R.string.action_ok),
            activity.getString(R.string.action_cancel),
            object : BottomSheetUtils.RadioOptionConsumer {
                override fun accept(option: Int) {
                    MediaFileHandler.startShareActivity(activity, vaultFile, option > 0)
                }
            })
    }

}