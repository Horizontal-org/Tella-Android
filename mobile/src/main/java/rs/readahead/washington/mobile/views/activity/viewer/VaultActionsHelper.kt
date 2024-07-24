package rs.readahead.washington.mobile.views.activity.viewer

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.widget.Toolbar
import com.hzontal.tella_vault.VaultFile
import org.hzontal.shared_ui.bottomsheet.BottomSheetUtils
import org.hzontal.shared_ui.bottomsheet.VaultSheetUtils
import org.hzontal.shared_ui.bottomsheet.VaultSheetUtils.showVaultActionsSheet
import permissions.dispatcher.NeedsPermission
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.media.MediaFileHandler
import rs.readahead.washington.mobile.views.activity.viewer.PermissionsActionsHelper.hasStoragePermissions
import rs.readahead.washington.mobile.views.activity.viewer.PermissionsActionsHelper.requestStoragePermissions
import rs.readahead.washington.mobile.views.base_ui.BaseActivity
import rs.readahead.washington.mobile.views.fragment.vault.edit.VaultEditFragment
import rs.readahead.washington.mobile.views.fragment.vault.info.VaultInfoFragment

var withMetadata = false
lateinit var filePicker: ActivityResultLauncher<Intent>
lateinit var requestPermission: ActivityResultLauncher<Intent>
lateinit var chosenVaultFile: VaultFile
lateinit var sharedViewModel: SharedMediaFileViewModel
lateinit var toolBar: Toolbar

/**
 * Helper object for handling Vault actions
 * The VaultActionsHelper is a utility class designed to handle actions related to a VaultFile,
 * which represents a file stored securely in a vault.
 */
object VaultActionsHelper {


    /**
     * Show the Vault actions dialog, which displays available actions for the given VaultFile.
     *
     * @param vaultFile The VaultFile for which actions will be displayed.
     * @param viewModel The SharedMediaFileViewModel for performing actions on the VaultFile.
     * @param unitFunction A lambda function representing an action to be executed.
     * @param toolbar The Toolbar associated with the current activity.
     */
    fun BaseActivity.showVaultActionsDialog(
        vaultFile: VaultFile,
        viewModel: SharedMediaFileViewModel,
        unitFunction: () -> Unit,
        toolbar: Toolbar
    ) {
        chosenVaultFile = vaultFile
        sharedViewModel = viewModel
        toolBar = toolbar

        // Create an instance of the VaultSheetUtils.IVaultActions interface to handle Vault actions
        val vaultActions = object : VaultSheetUtils.IVaultActions {
            // The following methods are placeholders for Vault actions and can be implemented as needed
            // Implement the methods for upload, share, move, rename, save, info, delete

            /**
             * Placeholder for the upload action.
             */
            override fun upload() {
            }

            /**
             * Trigger the share action, possibly with temporary timeout changes.
             */
            override fun share() {
                this@showVaultActionsDialog.maybeChangeTemporaryTimeout {
                    shareMediaFile()
                }
            }

            /**
             * Placeholder for the move action.
             */
            override fun move() {
            }

            /**
             * Rename the VaultFile using a VaultSheetUtils.showVaultRenameSheet dialog.
             */
            override fun rename() {
                VaultSheetUtils.showVaultRenameSheet(
                    supportFragmentManager,
                    getString(R.string.Vault_RenameFile_SheetTitle),
                    getString(R.string.action_cancel),
                    getString(R.string.action_ok), this@showVaultActionsDialog,
                    chosenVaultFile.name
                ) { name: String? ->
                    name?.let { viewModel.run { renameVaultFile(chosenVaultFile.id, it) } }
                }

            }

            /**
             * Show a confirmation dialog to save the VaultFile to the device.
             */
            override fun save() {
                BottomSheetUtils.showConfirmSheet(
                    supportFragmentManager,
                    getString(R.string.gallery_save_to_device_dialog_title),
                    getString(R.string.gallery_save_to_device_dialog_expl),
                    getString(R.string.action_save),
                    getString(R.string.action_cancel),
                    object : BottomSheetUtils.ActionConfirmed {
                        override fun accept(isConfirmed: Boolean) {
                            if (isConfirmed) {
                                exportMediaFile()
                            }
                        }
                    }
                )
            }

            /**
             * Show the information of the VaultFile using a VaultInfoFragment.
             */
            override fun info() {
                unitFunction()
                toolBar.title = getString(R.string.Vault_FileInfo)
                toolBar.menu.findItem(R.id.menu_item_more).isVisible = false
                toolBar.menu.findItem(R.id.menu_item_metadata).isVisible = false
                invalidateOptionsMenu()
                addFragment(
                    vaultFile.let { VaultInfoFragment.newInstance(it, false) },
                    R.id.container
                )
            }

            /**
             * Edit the file of the VaultFile using a VaultEditFragment.
             */
            override fun edit() {
                unitFunction()
                toolBar.visibility = View.GONE
                addFragment(
                    vaultFile.let { VaultEditFragment.newInstance(it, false) },
                    R.id.container
                )
            }

            /**
             * Show a confirmation dialog to delete the VaultFile.
             */
            override fun delete() {
                BottomSheetUtils.showConfirmSheet(
                    supportFragmentManager,
                    getString(R.string.Vault_DeleteFile_SheetTitle),
                    getString(R.string.Vault_deleteFile_SheetDesc),
                    getString(R.string.action_delete),
                    getString(R.string.action_cancel),
                    object : BottomSheetUtils.ActionConfirmed {
                        override fun accept(isConfirmed: Boolean) {
                            if (isConfirmed) {
                                vaultFile.let { sharedViewModel.confirmDeleteMediaFile(it) }
                            }
                        }
                    }
                )
            }

        }
        // Show the Vault actions dialog with available actions for the given VaultFile
        showVaultActionsSheet(
            supportFragmentManager,
            chosenVaultFile.name,
            getString(R.string.Vault_Upload_SheetAction),
            getString(R.string.Vault_Share_SheetAction),
            getString(R.string.Vault_Move_SheetDesc),
            getString(R.string.Vault_Rename_SheetAction),
            getString(R.string.gallery_action_desc_save_to_device),
            getString(R.string.Vault_File_SheetAction),
            getString(R.string.Vault_Delete_SheetAction),
            getString(R.string.Vault_FileEdit_SheetAction),
            isDirectory = false,
            isMultipleFiles = false,
            isUploadVisible = false,
            isMoveVisible = false,
            action = vaultActions
        )
    }

    fun BaseActivity.shareMediaFile() {
        if (chosenVaultFile == null) {
            return
        }
        if (chosenVaultFile?.metadata != null) {
            showShareWithMetadataDialog()
        } else {
            startShareActivity(false)
        }
    }

    private fun BaseActivity.showShareWithMetadataDialog() {
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
                    startShareActivity(option > 0)
                }
            })
    }

    private fun BaseActivity.startShareActivity(
        includeMetadata: Boolean
    ) {
        if (chosenVaultFile == null) {
            return
        }
        MediaFileHandler.startShareActivity(this, chosenVaultFile, includeMetadata)
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
                            performFileSearch(
                                chosenVaultFile, withMetadata,
                                sharedViewModel, filePicker, requestPermission
                            )
                        }
                    }
                })
        }
    }

    @NeedsPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    fun BaseActivity.exportMediaFile() {
        if (chosenVaultFile.metadata != null && withMetadata) {
            showExportWithMetadataDialog()
        } else {
            withMetadata = false
            maybeChangeTemporaryTimeout {
                performFileSearch(
                    chosenVaultFile, withMetadata,
                    sharedViewModel, filePicker, requestPermission
                )
            }
        }
    }

    // File search logic here
    fun BaseActivity.performFileSearch(
        vaultFile: VaultFile?,
        withMetadata: Boolean,
        viewModel: SharedMediaFileViewModel,
        filePickerLauncher: ActivityResultLauncher<Intent>,
        requestPermissionLauncher: ActivityResultLauncher<Intent>
    ) {
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
                vaultFile?.let {
                    viewModel.exportNewMediaFile(
                        withMetadata = withMetadata,
                        vaultFile = it,
                        path = null
                    )
                }
            }
        } else {
            requestStoragePermissions(requestPermissionLauncher)
        }
    }


}