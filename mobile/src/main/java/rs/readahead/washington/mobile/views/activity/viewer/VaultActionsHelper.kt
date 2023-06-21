package rs.readahead.washington.mobile.views.activity.viewer

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.os.Handler
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import com.hzontal.tella_vault.VaultFile
import org.hzontal.shared_ui.bottomsheet.BottomSheetUtils
import org.hzontal.shared_ui.bottomsheet.VaultSheetUtils
import org.hzontal.shared_ui.bottomsheet.VaultSheetUtils.showVaultActionsSheet
import permissions.dispatcher.NeedsPermission
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.data.sharedpref.Preferences
import rs.readahead.washington.mobile.media.MediaFileHandler
import rs.readahead.washington.mobile.util.LockTimeoutManager
import rs.readahead.washington.mobile.views.activity.viewer.VaultActionsHelper.showExportWithMetadataDialog
import rs.readahead.washington.mobile.views.activity.viewer.VaultActionsHelper.showVaultActionsDialog
import rs.readahead.washington.mobile.views.base_ui.BaseActivity
import rs.readahead.washington.mobile.views.fragment.vault.info.VaultInfoFragment

var withMetadata = false

object VaultActionsHelper {
    fun BaseActivity.showVaultActionsDialog(
        vaultFile: VaultFile?,
        viewModel: SharedMediaFileViewModel,
        unitFunction: () -> Unit,
        toolbar: Toolbar,
        requestPermissionLauncher: ActivityResultLauncher<String>,
        filePickerLauncher: ActivityResultLauncher<Intent>
    ) {
        val vaultActions = object : VaultSheetUtils.IVaultActions {
            // Implement the methods for upload, share, move, rename, save, info, delete
            override fun upload() {

            }

            override fun share() {
                this@showVaultActionsDialog.maybeChangeTemporaryTimeout {
                    shareMediaFile(vaultFile)
                }
            }

            override fun move() {
            }

            override fun rename() {
                VaultSheetUtils.showVaultRenameSheet(
                    supportFragmentManager,
                    getString(R.string.Vault_CreateFolder_SheetAction),
                    getString(R.string.action_cancel),
                    getString(R.string.action_ok),this@showVaultActionsDialog,
                    vaultFile?.name
                ) { name: String? ->
                    viewModel.renameVaultFile(vaultFile?.id, name)
                }

            }

            override fun save() {
                BottomSheetUtils.showConfirmSheet(
                    supportFragmentManager,
                    getString(R.string.gallery_save_to_device_dialog_title),
                    getString(R.string.gallery_save_to_device_dialog_expl),
                    getString(R.string.action_save),
                    getString(R.string.action_cancel),
                    object : BottomSheetUtils.ActionConfirmed {
                        override fun accept(isConfirmed: Boolean) {
                            exportMediaFile(vaultFile, withMetadata,viewModel,filePickerLauncher,requestPermissionLauncher)
                        }
                    }
                )
            }

            override fun info() {
                unitFunction()
                toolbar.title = getString(R.string.Vault_FileInfo)
                toolbar.menu.findItem(R.id.menu_item_more).isVisible = false
                toolbar.menu.findItem(R.id.menu_item_metadata).isVisible = false
                invalidateOptionsMenu()
                vaultFile?.let { VaultInfoFragment().newInstance(it, false) }
                    ?.let { addFragment(it, R.id.container) }
            }

            override fun delete() {
                BottomSheetUtils.showConfirmSheet(
                    supportFragmentManager,
                    getString(R.string.Vault_DeleteFile_SheetTitle),
                    getString(R.string.Vault_deleteFile_SheetDesc),
                    getString(R.string.action_delete),
                    getString(R.string.action_cancel),
                    object : BottomSheetUtils.ActionConfirmed {
                        override fun accept(isConfirmed: Boolean) {
                            vaultFile?.let { viewModel.confirmDeleteMediaFile(it) }
                        }
                    }
                )
            }

        }

        showVaultActionsSheet(
            supportFragmentManager,
            vaultFile?.name,
            getString(R.string.Vault_Upload_SheetAction),
            getString(R.string.Vault_Share_SheetAction),
            getString(R.string.Vault_Move_SheetDesc),
            getString(R.string.Vault_Rename_SheetAction),
            getString(R.string.gallery_action_desc_save_to_device),
            getString(R.string.Vault_File_SheetAction),
            getString(R.string.Vault_Delete_SheetAction),
            isDirectory = false,
            isMultipleFiles = false,
            isUploadVisible = false,
            isMoveVisible = false,
            action = vaultActions
        )
    }

    fun BaseActivity.shareMediaFile(vaultFile: VaultFile?) {
        if (vaultFile == null) {
            return
        }
        if (vaultFile?.metadata != null) {
            showShareWithMetadataDialog(vaultFile)
        } else {
            startShareActivity(false,vaultFile)
        }
    }
    private fun BaseActivity.showShareWithMetadataDialog(vaultFile: VaultFile?) {
        val options = mapOf(
            R.string.verification_share_select_media_and_verification to R.string.verification_share_select_media_and_verification,
            R.string.verification_share_select_only_media to R.string.verification_share_select_only_media
        )
        BottomSheetUtils.showRadioListOptionsSheet(supportFragmentManager,
            this,
            options as LinkedHashMap<Int, Int>,
            getString(R.string.verification_share_dialog_title),
            getString(R.string.verification_share_dialog_expl),
            getString(R.string.action_ok),
            getString(R.string.action_cancel),
            object : BottomSheetUtils.RadioOptionConsumer {
                override fun accept(option: Int) {
                    startShareActivity(option > 0,vaultFile)
                }
            })
    }
    private fun BaseActivity.startShareActivity(
        includeMetadata: Boolean,
        vaultFile: VaultFile?) {
        if (vaultFile == null) {
            return
        }
        MediaFileHandler.startShareActivity(this, vaultFile, includeMetadata)
    }




    internal fun BaseActivity.showExportWithMetadataDialog() {
        val options = mapOf(
            R.string.verification_share_select_media_and_verification to R.string.verification_share_select_media_and_verification,
            R.string.verification_share_select_only_media to R.string.verification_share_select_only_media
        )
        Handler().post {
            BottomSheetUtils.showRadioListOptionsSheet(supportFragmentManager,
                this,
                options as LinkedHashMap<Int, Int>,
                getString(R.string.verification_share_dialog_title),
                getString(R.string.verification_share_dialog_expl),
                getString(R.string.action_ok),
                getString(R.string.action_cancel),
                object : BottomSheetUtils.RadioOptionConsumer {
                    override fun accept(option: Int) {
                        withMetadata = option > 0
                        this@showExportWithMetadataDialog.maybeChangeTemporaryTimeout {
                            //performFileSearch()
                        }
                    }
                })
        }
    }

    @NeedsPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    fun BaseActivity.exportMediaFile(vaultFile: VaultFile?, withMetadata: Boolean,viewModel: SharedMediaFileViewModel,filePickerLauncher: ActivityResultLauncher<Intent>,requestPermissionLauncher: ActivityResultLauncher<String>) {
        if (vaultFile?.metadata != null && withMetadata) {
            showExportWithMetadataDialog()
        } else {
            performFileSearch(vaultFile,withMetadata,viewModel,filePickerLauncher,requestPermissionLauncher)
        }
    }

    // File search logic here
    private fun BaseActivity.performFileSearch(vaultFile: VaultFile?,withMetadata: Boolean,viewModel: SharedMediaFileViewModel,filePickerLauncher: ActivityResultLauncher<Intent>,requestPermissionLauncher: ActivityResultLauncher<String>) {
        if (hasStoragePermissions(this)) {
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
                    addFlags(
                        Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION or
                                Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                                Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                }
                filePickerLauncher.launch(intent)
            } else {
                vaultFile?.let { viewModel.exportNewMediaFile(withMetadata, it, null) }
            }
        } else {
            requestStoragePermissions(requestPermissionLauncher)
        }
    }

    // Check if the app has storage permissions
    private fun hasStoragePermissions(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            val result: Int = ContextCompat.checkSelfPermission(
                context, Manifest.permission.READ_EXTERNAL_STORAGE
            )
            val result1: Int = ContextCompat.checkSelfPermission(
                context, Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
            result == PackageManager.PERMISSION_GRANTED && result1 == PackageManager.PERMISSION_GRANTED
        }
    }


    private fun BaseActivity.requestStoragePermissions(requestPermissionLauncher: ActivityResultLauncher<String>) {
        this.maybeChangeTemporaryTimeout()
        requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    }

}