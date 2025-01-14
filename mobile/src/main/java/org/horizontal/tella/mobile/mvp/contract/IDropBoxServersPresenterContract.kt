package org.horizontal.tella.mobile.mvp.contract

import android.content.Context
import org.horizontal.tella.mobile.domain.entity.dropbox.DropBoxServer

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