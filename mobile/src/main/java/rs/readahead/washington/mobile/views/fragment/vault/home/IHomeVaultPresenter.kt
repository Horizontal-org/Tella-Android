package rs.readahead.washington.mobile.views.fragment.vault.home

import android.content.Context
import com.hzontal.tella_vault.VaultFile
import com.hzontal.tella_vault.filter.FilterType
import com.hzontal.tella_vault.filter.Limits
import com.hzontal.tella_vault.filter.Sort
import rs.readahead.washington.mobile.domain.entity.collect.CollectForm
import rs.readahead.washington.mobile.mvp.contract.IBasePresenter

class IHomeVaultPresenter {
    interface IView {
        fun getContext(): Context?
        fun onCountTUServersEnded(num: Long?)
        fun onCountTUServersFailed(throwable: Throwable?)
        fun onCountCollectServersEnded(num: Long?)
        fun onCountCollectServersFailed(throwable: Throwable?)
        fun onGetFilesSuccess(files: List<VaultFile?>)
        fun onGetFilesError(error: Throwable?)
        fun onMediaExported(num: Int)
        fun onExportError(error: Throwable?)
        fun onExportStarted()
        fun onExportEnded()
        fun onGetFavoriteCollectFormsSuccess(files: List<CollectForm>)
        fun onGetFavoriteCollectFormsError(error: Throwable?)
    }

    interface IPresenter : IBasePresenter {
        fun executePanicMode()
        fun countTUServers()
        fun countCollectServers()
        fun exportMediaFiles(vaultFiles: List<VaultFile?>)
        fun getRecentFiles(filterType: FilterType?, sort: Sort?,limits: Limits)
        fun getFavoriteCollectForms()
    }
}