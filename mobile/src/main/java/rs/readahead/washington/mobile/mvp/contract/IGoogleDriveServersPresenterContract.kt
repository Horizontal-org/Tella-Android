package rs.readahead.washington.mobile.mvp.contract

import android.content.Context
import rs.readahead.washington.mobile.domain.entity.UWaziUploadServer
import rs.readahead.washington.mobile.domain.entity.googledrive.GoogleDriveServer

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
        fun getGoogleDriveServers()
        fun create(server: GoogleDriveServer)
        fun remove(server: GoogleDriveServer)
    }
}