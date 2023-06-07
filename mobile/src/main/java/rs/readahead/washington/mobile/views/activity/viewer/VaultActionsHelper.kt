package rs.readahead.washington.mobile.views.activity.viewer

import android.view.View
import androidx.appcompat.widget.Toolbar
import com.hzontal.tella_vault.VaultFile
import org.hzontal.shared_ui.bottomsheet.BottomSheetUtils
import org.hzontal.shared_ui.bottomsheet.VaultSheetUtils
import org.hzontal.shared_ui.bottomsheet.VaultSheetUtils.showVaultActionsSheet
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.views.fragment.vault.info.VaultInfoFragment

object VaultActionsHelper {
    fun showVaultActionsDialog(
        activity: VideoViewerActivity,
        vaultFile: VaultFile?,
        viewModel: SharedMediaFileViewModel,
        unitFunction: () -> Unit,
        toolbar: Toolbar
    ) {
        val vaultActions = object : VaultSheetUtils.IVaultActions {
            // Implement the methods for upload, share, move, rename, save, info, delete
            override fun upload() {

            }

            override fun share() {
                activity.maybeChangeTemporaryTimeout {
                    activity.shareMediaFile()
                }
            }

            override fun move() {

            }

            override fun rename() {
                VaultSheetUtils.showVaultRenameSheet(
                    activity.supportFragmentManager,
                    activity.getString(R.string.Vault_CreateFolder_SheetAction),
                    activity.getString(R.string.action_cancel),
                    activity.getString(R.string.action_ok),
                    activity,
                    vaultFile?.name
                ) { name: String? ->
                    viewModel.renameVaultFile(vaultFile?.id, name)
                }

            }

            override fun save() {
                BottomSheetUtils.showConfirmSheet(
                    activity.supportFragmentManager,
                    activity.getString(R.string.gallery_save_to_device_dialog_title),
                    activity.getString(R.string.gallery_save_to_device_dialog_expl),
                    activity.getString(R.string.action_save),
                    activity.getString(R.string.action_cancel),
                    object : BottomSheetUtils.ActionConfirmed {
                        override fun accept(isConfirmed: Boolean) {
                            activity.exportMediaFile()
                        }
                    }
                )
            }

            override fun info() {
                unitFunction()
                toolbar.title = activity.getString(R.string.Vault_FileInfo)
                toolbar.menu.findItem(R.id.menu_item_more).isVisible = false
                toolbar.menu.findItem(R.id.menu_item_metadata).isVisible = false
                activity.invalidateOptionsMenu()
                vaultFile?.let { VaultInfoFragment().newInstance(it, false) }
                    ?.let { activity.addFragment(it, R.id.container) }
            }

            override fun delete() {
                BottomSheetUtils.showConfirmSheet(
                    activity.supportFragmentManager,
                    activity.getString(R.string.Vault_DeleteFile_SheetTitle),
                    activity.getString(R.string.Vault_deleteFile_SheetDesc),
                    activity.getString(R.string.action_delete),
                    activity.getString(R.string.action_cancel),
                    object : BottomSheetUtils.ActionConfirmed {
                        override fun accept(isConfirmed: Boolean) {
                            viewModel.deleteMediaFiles(vaultFile)
                        }
                    }
                )
            }

        }

        showVaultActionsSheet(
            activity.supportFragmentManager,
            vaultFile?.name,
            activity.getString(R.string.Vault_Upload_SheetAction),
            activity.getString(R.string.Vault_Share_SheetAction),
            activity.getString(R.string.Vault_Move_SheetDesc),
            activity.getString(R.string.Vault_Rename_SheetAction),
            activity.getString(R.string.gallery_action_desc_save_to_device),
            activity.getString(R.string.Vault_File_SheetAction),
            activity.getString(R.string.Vault_Delete_SheetAction),
            isDirectory = false,
            isMultipleFiles = false,
            isUploadVisible = false,
            isMoveVisible = false,
            action = vaultActions
        )
    }


}