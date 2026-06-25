package org.horizontal.tella.mobile.views.fragment.connections.attachments

import android.content.Context
import android.content.Intent
import com.hzontal.tella_vault.filter.FilterType

const val RETURN_ODK = "rodk"
const val VAULT_FILE_KEY = "vfk"
const val VAULT_FILES_FILTER = "vff"
const val VAULT_PICKER_SINGLE = "vps"

object VaultFilePickerContract {
    @JvmStatic
    @JvmOverloads
    fun createIntent(
        context: Context,
        filterType: FilterType,
        singleSelection: Boolean,
        selectedVaultFileIdsJson: String? = null,
        returnOdkMediaFile: Boolean = false,
    ): Intent {
        return Intent(context, AttachmentsActivitySelector::class.java)
            .putExtra(VAULT_FILES_FILTER, filterType)
            .putExtra(VAULT_PICKER_SINGLE, singleSelection)
            .putExtra(RETURN_ODK, returnOdkMediaFile)
            .apply {
                selectedVaultFileIdsJson?.let { putExtra(VAULT_FILE_KEY, it) }
            }
    }
}
