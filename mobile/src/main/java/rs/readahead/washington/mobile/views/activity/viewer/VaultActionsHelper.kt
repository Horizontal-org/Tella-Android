package rs.readahead.washington.mobile.views.activity.viewer

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.provider.Settings
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
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
import rs.readahead.washington.mobile.views.base_ui.BaseActivity
import rs.readahead.washington.mobile.views.fragment.vault.attachements.helpers.WRITE_REQUEST_CODE
import rs.readahead.washington.mobile.views.fragment.vault.info.VaultInfoFragment

var withMetadata = false
lateinit var filePicker: ActivityResultLauncher<Intent>
lateinit var requestPermission: ActivityResultLauncher<Intent>
lateinit var chosenVaultFile: VaultFile
lateinit var sharedViewModel: SharedMediaFileViewModel
lateinit var toolBar: Toolbar

object VaultActionsHelper {

    fun BaseActivity.initContracts() {
        requestPermission =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == AppCompatActivity.RESULT_OK) {
                    // Permission granted, perform the necessary actions
                    LockTimeoutManager().lockTimeout = Preferences.getLockTimeout()
                    performFileSearch(
                        chosenVaultFile, withMetadata, sharedViewModel, filePicker,
                        requestPermission
                    )
                } else {
                    // Permission denied, handle accordingly
                }
            }
        filePicker =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == AppCompatActivity.RESULT_OK) {
                    assert(result.data != null)
                    chosenVaultFile?.let {
                        sharedViewModel.exportNewMediaFile(
                            withMetadata,
                            it,
                            result.data?.data
                        )
                    }
                }
            }


    }

    fun BaseActivity.showVaultActionsDialog(
        vaultFile: VaultFile,
        viewModel: SharedMediaFileViewModel,
        unitFunction: () -> Unit,
        toolbar: Toolbar
    ) {
        chosenVaultFile = vaultFile
        sharedViewModel = viewModel
        toolBar = toolbar

        val vaultActions = object : VaultSheetUtils.IVaultActions {
            // Implement the methods for upload, share, move, rename, save, info, delete
            override fun upload() {
            }

            override fun share() {
                this@showVaultActionsDialog.maybeChangeTemporaryTimeout {
                    shareMediaFile()
                }
            }

            override fun move() {
            }

            override fun rename() {
                VaultSheetUtils.showVaultRenameSheet(
                    supportFragmentManager,
                    getString(R.string.Vault_CreateFolder_SheetAction),
                    getString(R.string.action_cancel),
                    getString(R.string.action_ok), this@showVaultActionsDialog,
                    chosenVaultFile?.name
                ) { name: String? ->
                    viewModel.renameVaultFile(chosenVaultFile?.id, name)
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
                            exportMediaFile()
                        }
                    }
                )
            }

            override fun info() {
                unitFunction()
                toolBar.title = getString(R.string.Vault_FileInfo)
                toolBar.menu.findItem(R.id.menu_item_more).isVisible = false
                toolBar.menu.findItem(R.id.menu_item_metadata).isVisible = false
                invalidateOptionsMenu()
                addFragment(vaultFile?.let { VaultInfoFragment.newInstance(it, false) }, R.id.container)
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
                            vaultFile?.let { sharedViewModel.confirmDeleteMediaFile(it) }
                        }
                    }
                )
            }

        }

        showVaultActionsSheet(
            supportFragmentManager,
            chosenVaultFile?.name,
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
                                sharedViewModel, filePicker, requestPermission)
                        }
                    }
                })
        }
    }

    @NeedsPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    fun BaseActivity.exportMediaFile() {
        if (chosenVaultFile?.metadata != null && withMetadata) {
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
    private fun BaseActivity.performFileSearch(
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


    private fun BaseActivity.requestStoragePermissions(requestPermissionLauncher: ActivityResultLauncher<Intent>) {
        this.maybeChangeTemporaryTimeout()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                .addCategory(Intent.CATEGORY_DEFAULT)
                .setData(Uri.parse("package:${application.packageName}"))
            requestPermissionLauncher.launch(intent)
        } else {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), WRITE_REQUEST_CODE
            )
        }
    }

}