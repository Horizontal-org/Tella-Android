package rs.readahead.washington.mobile.mvp.contract

import android.content.Context
import rs.readahead.washington.mobile.domain.entity.dropbox.DropBoxServer

class IDropBoxServersPresenterContract {
    interface IView {
        val context: Context?
        fun showLoading()
        fun hideLoading()
        fun onDropBoxServersLoaded(dropBoxServerServers: List<DropBoxServer>)
        fun onLoadDropBoxServersError(throwable: Throwable)
        fun onCreatedDropBoxServer(server: DropBoxServer)
        fun onCreateDropBoxServerError(throwable: Throwable)
        fun onRemovedDropBoxServer(server: DropBoxServer)
        fun onRemoveDropBoxServerError(throwable: Throwable)
    }

    interface IPresenter : IBasePresenter {
        fun getDropBoxServers()
        fun create(server: DropBoxServer)
        fun remove(server: DropBoxServer)
    }
}