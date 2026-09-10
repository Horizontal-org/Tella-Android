package org.horizontal.tella.mobile.util

import com.hzontal.tella_vault.VaultFile
import com.hzontal.tella_vault.database.VaultDataSource
import com.hzontal.tella_vault.rx.RxVault
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import org.horizontal.tella.mobile.MyApplication

object VaultFolderPath {
    const val ROOT = "/"
    private const val MAX_DEPTH = 64

    @JvmStatic
    fun format(folderNamesFromRoot: List<String>): String {
        val names = folderNamesFromRoot.filter { it.isNotBlank() }
        if (names.isEmpty()) {
            return ROOT
        }
        return names.joinToString(separator = "/", prefix = "/")
    }

    @JvmStatic
    fun resolve(fileId: String?, lookup: (String) -> VaultFile?): String {
        if (fileId.isNullOrBlank() || fileId == VaultDataSource.ROOT_UID) {
            return ROOT
        }
        val names = ArrayList<String>()
        var currentId: String? = fileId
        var depth = 0
        while (!currentId.isNullOrBlank() &&
            currentId != VaultDataSource.ROOT_UID &&
            depth++ < MAX_DEPTH
        ) {
            val id = currentId ?: break
            val current = lookup(id) ?: break
            val parentId = current.parentId
            if (parentId.isNullOrBlank() || parentId == VaultDataSource.ROOT_UID) {
                break
            }
            val parent = lookup(parentId) ?: break
            val name = parent.name
            if (!name.isNullOrBlank()) {
                names.add(0, name)
            }
            currentId = parentId
        }
        return format(names)
    }

    @JvmStatic
    fun fileLocation(folderPath: String, fileName: String?): String {
        val name = fileName?.takeIf { it.isNotBlank() } ?: return folderPath.ifBlank { ROOT }
        val folder = folderPath.ifBlank { ROOT }
        return if (folder == ROOT) "$ROOT$name" else "$folder/$name"
    }

    @JvmStatic
    fun folderPath(folderId: String?, lookup: (String) -> VaultFile?): String {
        if (folderId.isNullOrBlank() || folderId == VaultDataSource.ROOT_UID) {
            return ROOT
        }
        val folder = lookup(folderId) ?: return ROOT
        return fileLocation(resolve(folderId, lookup), folder.name)
    }

    @JvmStatic
    fun fileLocationInFolder(folderId: String?, fileName: String?, vault: RxVault): String {
        return fileLocation(folderPath(folderId) { id -> lookupFile(vault, id) }, fileName)
    }

    @JvmStatic
    fun resolveAsync(fileId: String?): Single<String> {
        return MyApplication.keyRxVault.rxVault
            .firstOrError()
            .map { vault -> resolve(fileId) { id -> lookupFile(vault, id) } }
            .subscribeOn(Schedulers.io())
    }

    private fun lookupFile(vault: RxVault, id: String): VaultFile? {
        return try {
            vault.get(id).blockingGet()
        } catch (_: Exception) {
            null
        }
    }
}
