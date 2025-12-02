package org.horizontal.tella.mobile.views.activity.viewer

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.horizontal.tella.mobile.data.sharedpref.Preferences
import org.horizontal.tella.mobile.util.LockTimeoutManager
import org.horizontal.tella.mobile.views.activity.viewer.VaultActionsHelper.performFileSearch
import org.horizontal.tella.mobile.views.base_ui.BaseActivity
import org.horizontal.tella.mobile.views.fragment.vault.attachements.helpers.WRITE_REQUEST_CODE

object PermissionsActionsHelper {
    // Initialize contracts for handling activity results
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
        // Contract for picking files
        filePicker =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == AppCompatActivity.RESULT_OK) {
                    assert(result.data != null)
                    // Export the chosen VaultFile to the selected file destination
                    chosenVaultFile.let {
                        sharedViewModel.exportNewMediaFile(
                            withMetadata,
                            it,
                            result.data?.data
                        )
                    }
                }
            }
    }


    // Check if the app has storage permissions
    fun hasStoragePermissions(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // On Android 10+ we rely exclusively on SAF, no “all files” permission required
            true
        } else {
            val read = ContextCompat.checkSelfPermission(
                context, Manifest.permission.READ_EXTERNAL_STORAGE
            )
            val write = ContextCompat.checkSelfPermission(
                context, Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
            read == PackageManager.PERMISSION_GRANTED && write == PackageManager.PERMISSION_GRANTED
        }
    }


    fun BaseActivity.requestStoragePermissions(
        requestPermissionLauncher: ActivityResultLauncher<Intent>
    ) {
        this.maybeChangeTemporaryTimeout()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // No-op on Android 10+ for SAF exports
            // You can optionally show a message if needed, but no permission needed
            return
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                WRITE_REQUEST_CODE
            )
        }
    }

}