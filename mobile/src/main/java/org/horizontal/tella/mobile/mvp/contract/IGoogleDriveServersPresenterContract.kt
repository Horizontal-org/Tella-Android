package org.horizontal.tella.mobile.mvp.contract

import android.content.Context
import org.horizontal.tella.mobile.domain.entity.googledrive.GoogleDriveServer

class IGoogleDriveServersPresenterContract {

    interface IView {
        val context: Context?
        fun showLoading()
        fun hideLoading()
        fun onGoogleDriveServersLoaded(googleDriveServers: List<GoogleDriveServer>)
        fun onLoadGoogleDriveServersError(throwable: Throwable)
        fun onCreatedGoogleDriveServer(server: GoogleDriveServer)
        fun onCreateGoogleDriveServerError(throwable: Throwable)
        fun onRemovedGoogleDriveServer(server: GoogleDriveServer)
        fun onRemoveGoogleDriveServerError(throwable: Throwable)
    }

    interface IPresenter : IBasePresenter {
        fun getGoogleDriveServers(googleDriveId: String)
        fun create(server: GoogleDriveServer)
        fun remove(server: GoogleDriveServer)
    }
}