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


     fun BaseActivity.requestStoragePermissions(requestPermissionLauncher: ActivityResultLauncher<Intent>) {
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