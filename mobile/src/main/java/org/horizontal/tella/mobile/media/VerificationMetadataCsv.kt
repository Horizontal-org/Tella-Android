package org.horizontal.tella.mobile.media

import com.hzontal.tella_vault.VaultFile
import com.hzontal.tella_vault.database.VaultDataSource
import org.horizontal.tella.mobile.presentation.entity.mapper.PublicMetadataMapper
import java.nio.charset.StandardCharsets

/**
 * CSV export of a vault file's verification metadata.
 * The file is named after the original so it stays easy to match after share, export, or re-import.
 */
object VerificationMetadataCsv {
    const val MIME_TYPE = "text/csv"

    fun fileNameFor(originalName: String?): String {
        val name = originalName?.takeIf { it.isNotBlank() } ?: "file"
        val dot = name.lastIndexOf('.')
        val base = if (dot > 0) name.substring(0, dot) else name
        return "$base.csv"
    }

    fun parentFolderId(parentId: String?, vaultFileParentId: String?): String {
        return parentId?.takeIf { it.isNotBlank() }
            ?: vaultFileParentId?.takeIf { it.isNotBlank() }
            ?: VaultDataSource.ROOT_UID
    }

    fun existingIn(siblings: List<VaultFile>?, originalName: String?): VaultFile? {
        val csvName = fileNameFor(originalName)
        return siblings?.firstOrNull { it.name == csvName }
    }

    fun existingCsv(linkedBySource: VaultFile?, siblings: List<VaultFile>?, originalName: String?): VaultFile? {
        return linkedBySource ?: existingIn(siblings, originalName)
    }

    fun toCsvBytes(vaultFile: VaultFile): ByteArray {
        val map = PublicMetadataMapper.transformToMap(vaultFile)
        val csv = buildString {
            append(map.keys.joinToString(","))
            append('\n')
            append(map.values.joinToString(","))
            append('\n')
        }
        return csv.toByteArray(StandardCharsets.UTF_8)
    }
}
