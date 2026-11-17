@file:JvmName("VaultFileIcon")

package org.horizontal.tella.mobile.views.fragment.vault.adapters

import androidx.annotation.DrawableRes
import com.hzontal.utils.MediaFile.isCsvFile
import org.horizontal.tella.mobile.R

@JvmOverloads
fun vaultDocumentIcon(
    name: String?,
    mimeType: String?,
    @DrawableRes fallback: Int = R.drawable.ic_document_24px_filled
): Int {
    return if (isCsvFile(name, mimeType)) R.drawable.ic_csv else fallback
}
