package rs.readahead.washington.mobile.views.fragment.vault.attachements

import android.content.Context
import android.net.Uri
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
        fun onMediaExported(num: Int)
        fun onExportError(error: Throwable?)
        fun onExportStarted()
        fun onExportEnded()
        fun onCountTUServersEnded(num: Long?)
        fun onCountTUServersFailed(throwable: Throwable?)
        fun getContext(): Context?
    }

    interface IPresenter : IBasePresenter {
        fun getFiles(filter: IVaultDatabase.Filter?, sort: IVaultDatabase.Sort?)
        fun importImage(uri: Uri?)
        fun importVideo(uri: Uri?)
        fun addNewMediaFile(vaultFile: VaultFile?)
        fun deleteMediaFiles(mediaFiles: List<VaultFile?>?)
        fun exportMediaFiles(mediaFiles: List<VaultFile?>?)
        fun countTUServers() //void encryptTmpVideo(Uri uri);
    }
}