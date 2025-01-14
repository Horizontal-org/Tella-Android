package org.horizontal.tella.mobile.views.fragment.vault.attachements.helpers

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import androidx.core.content.ContextCompat
import com.hzontal.tella_vault.VaultFile
import com.hzontal.tella_vault.filter.FilterType
import org.hzontal.shared_ui.appbar.ToolbarComponent
import org.horizontal.tella.mobile.R
import org.horizontal.tella.mobile.media.MediaFileHandler
import org.horizontal.tella.mobile.views.base_ui.BaseActivity
import org.horizontal.tella.mobile.views.fragment.vault.attachements.helpers.AttachmentsSheetHelper.showShareFileWithMetadataDialog
import org.horizontal.tella.mobile.views.fragment.vault.attachements.helpers.AttachmentsSheetHelper.showShareWithMetadataDialog

object AttachmentsHelper {

    internal fun hasStoragePermissions(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            val result: Int = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
            val result1: Int = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
            result == PackageManager.PERMISSION_GRANTED && result1 == PackageManager.PERMISSION_GRANTED
        }
    }

    internal fun shareVaultFiles(selected: List<VaultFile>, activity: BaseActivity) {
        if (selected.isNullOrEmpty()) return

        val hasMetadata = selected.any { file -> file.metadata != null }

        if (hasMetadata) {
            showShareWithMetadataDialog(activity, selected)
        } else {
            startShareActivity(false, selected, activity)
        }
    }

    internal fun setToolbarLabel(
        filterType: FilterType,
        toolbarComponent: ToolbarComponent,
        activity: BaseActivity
    ) {
        when (filterType) {
            FilterType.PHOTO -> toolbarComponent.setStartTextTitle(activity.getString(R.string.Vault_Images_Title))
            FilterType.VIDEO -> toolbarComponent.setStartTextTitle(activity.getString(R.string.Vault_Videos_Title))
            FilterType.AUDIO -> toolbarComponent.setStartTextTitle(activity.getString(R.string.Vault_Audios_Title))
            FilterType.DOCUMENTS -> toolbarComponent.setStartTextTitle(activity.getString(R.string.Vault_Documents_Title))
            FilterType.OTHERS -> toolbarComponent.setStartTextTitle(activity.getString(R.string.Vault_Others_Title))
            FilterType.ALL -> toolbarComponent.setStartTextTitle(activity.getString(R.string.Vault_AllFiles_Title))
            FilterType.PHOTO_VIDEO -> toolbarComponent.setStartTextTitle(activity.getString(R.string.Vault_PhotosAndVideos_Title))
            else -> ""
        }
    }

    internal fun getCurrentType(filterType: FilterType): String {
        return when (filterType) {
            FilterType.ALL -> "*/*"
            FilterType.DOCUMENTS -> "application/*"
            FilterType.PHOTO -> "image/*"
            FilterType.VIDEO -> "video/*"
            FilterType.AUDIO -> "audio/*"
            FilterType.PHOTO_VIDEO -> "image/*|video/*"
            else -> "image/*"
        }
    }


    internal fun startShareActivity(
        includeMetadata: Boolean,
        selected: List<VaultFile>,
        activity: BaseActivity
    ) {
        if (selected.isNullOrEmpty()) return
        if (selected.size > 1) {
            val attachments = MediaFileHandler.walkAllFilesWithDirectories(selected)
            MediaFileHandler.startShareActivity(activity, attachments, includeMetadata)
        } else {
            if (selected[0].type == VaultFile.Type.DIRECTORY) {
                val attachments = MediaFileHandler.walkAllFilesWithDirectories(selected)
                MediaFileHandler.startShareActivity(activity, attachments, includeMetadata)
            } else {
                MediaFileHandler.startShareActivity(activity, selected[0], includeMetadata)
            }
        }
    }

    internal fun shareVaultFile(vaultFile: VaultFile?, activity: BaseActivity) {
        if (vaultFile == null) {
            return
        }

        if (vaultFile.metadata != null) {
            showShareFileWithMetadataDialog(vaultFile, activity)
        } else {
            MediaFileHandler.startShareActivity(activity, vaultFile, false)
        }
    }
}