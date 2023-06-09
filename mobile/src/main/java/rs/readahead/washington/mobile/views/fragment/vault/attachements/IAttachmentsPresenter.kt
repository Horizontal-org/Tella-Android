package rs.readahead.washington.mobile.views.fragment.vault.attachements

import android.content.Context
import android.net.Uri
import com.hzontal.tella_vault.VaultFile
import com.hzontal.tella_vault.filter.FilterType
import com.hzontal.tella_vault.filter.Sort
import rs.readahead.washington.mobile.mvp.contract.IBasePresenter

class IAttachmentsPresenter {
    interface IView {
        //fun onMediaImported(vaultFile: VaultFile)
       // fun onMediaImportedWithDelete(uri: Uri)
       // fun onConfirmDeleteFiles(vaultFiles: List<VaultFile?>, showConfirmDelete: Boolean)
        //fun onImportError(error: Throwable?)
        //fun onImportStarted()
      //  fun onImportEnded()
       // fun onMediaFilesAdded()
        fun onMediaFilesAddingError(error: Throwable?)
        fun onMediaFilesDeleted(num: Int)
        //fun onMediaExported(num: Int)
        //fun onExportError(error: Throwable?)
        //fun onExportStarted()
        //fun onExportEnded()
        //fun onCountTUServersEnded(num: Long?)
        //fun onCountTUServersFailed(throwable: Throwable?)
       // fun onRenameFileStart()
        //fun onRenameFileEnd()
        //fun onRenameFileSuccess()
        //fun onRenameFileError(error: Throwable?)
       // fun onCreateFolderSuccess()
      //  fun onGetRootIdSuccess(vaultFile: VaultFile?)
      //  fun onGetRootIdError(error: Throwable?)
        fun getContext(): Context?
      //  fun onGetProgressPercent(numberFilesImported: Double, totalFilesToImport: Int)
    }

    interface IPresenter : IBasePresenter {
       // fun getFiles(parent: String?, filterType: FilterType?, sort: Sort?)
        fun importVaultFiles(uris: List<Uri>, parentId: String?, deleteOriginal: Boolean)
        fun addNewVaultFiles()
        fun renameVaultFile(id: String, name: String)
        fun deleteVaultFiles(vaultFiles: List<VaultFile?>?)
        fun moveFiles(parentId: String?, vaultFiles: List<VaultFile?>?)
        fun deleteVaultFile(vaultFile: VaultFile?)
        fun exportMediaFiles(withMetadata: Boolean, vaultFiles: List<VaultFile?>, path: Uri?)
        fun deleteFilesAfterConfirmation(vaultFiles: List<VaultFile?>)
      ///  fun createFolder(folderName: String, parent: String)
        fun getRootId()
        fun countTUServers()
        fun cancelImportVaultFiles()
    }
}