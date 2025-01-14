package org.horizontal.tella.mobile.mvp.contract

import android.content.Context
import org.horizontal.tella.mobile.domain.entity.nextcloud.NextCloudServer

interface INextCloudServersPresenterContract {

    interface IView {
        val context: Context?
        fun showLoading()
        fun hideLoading()
        fun onNextCloudServersLoaded(nextCloudServers: List<NextCloudServer>)
        fun onLoadNextCloudServersError(throwable: Throwable)
        fun onCreatedNextCloudServer(server: NextCloudServer)
        fun onCreateNextCloudServerError(throwable: Throwable)
        fun onRemovedNextCloudServer(server: NextCloudServer)
        fun onRemoveNextCloudServerError(throwable: Throwable)
    }

    interface IPresenter : IBasePresenter {
        fun getNextCloudServers()
        fun create(server: NextCloudServer)
        fun remove(server: NextCloudServer)
    }
}