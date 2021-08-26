package rs.readahead.washington.mobile.views.fragment.vault.attachements

import android.content.Context
import android.net.Uri
import com.hzontal.tella_vault.Filter
import com.hzontal.tella_vault.IVaultDatabase
import com.hzontal.tella_vault.VaultFile
import rs.readahead.washington.mobile.mvp.contract.IBasePresenter

class IAttachmentsPresenter {
    interface IView {
        fun onGetFilesStart()
        fun onGetFilesEnd()
        fun onGetFilesSuccess(files: List<VaultFile?>)
        fun onGetFilesError(error: Throwable?)
        fun onMediaImported(vaultFile: VaultFile?)
        fun onImportError(error: Throwable?)
        fun onImportStarted()
        fun onImportEnded()
        fun onMediaFilesAdded(vaultFile: VaultFile?)
        fun onMediaFilesAddingError(error: Throwable?)
        fun onMediaFilesDeleted(num: Int)
        fun onMediaFilesDeletionError(throwable: Throwable?)
        fun onMediaFileDeleted()
        fun onMediaFileDeletionError(throwable: Throwable?)
        fun onMediaExported(num: Int)
        fun onExportError(error: Throwable?)
        fun onExportStarted()
        fun onExportEnded()
        fun onCountTUServersEnded(num: Long?)
        fun onCountTUServersFailed(throwable: Throwable?)
        fun onRenameFileStart()
        fun onRenameFileEnd()
        fun onRenameFileSuccess()
        fun onRenameFileError(error: Throwable?)
        fun getContext(): Context?
    }

    interface IPresenter : IBasePresenter {
        fun getFiles(filter: Filter?, sort: IVaultDatabase.Sort?)
        fun importImage(uri: Uri?)
        fun importVideo(uri: Uri?)
        fun addNewVaultFile(vaultFile: VaultFile?)
        fun renameVaultFile(id : String, name : String)
        fun deleteVaultFiles(vaultFiles: List<VaultFile?>?)
        fun deleteVaultFile(vaultFile: VaultFile?)
        fun exportMediaFiles(vaultFiles: List<VaultFile?>)
        fun countTUServers()

    }
}