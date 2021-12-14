package rs.readahead.washington.mobile.mvp.contract

import android.content.Context
import rs.readahead.washington.mobile.domain.entity.UWaziUploadServer
import rs.readahead.washington.mobile.domain.entity.UploadProgressInfo

class ICheckUwaziServerContract {
    interface IView {
        fun onServerCheckSuccess(server: UWaziUploadServer)
        fun onServerCheckFailure(status: UploadProgressInfo.Status)
        fun onServerCheckError(error: Throwable)
        fun showServerCheckLoading()
        fun hideServerCheckLoading()
        fun onNoConnectionAvailable()
        fun setSaveAnyway(enabled: Boolean)
        fun getContext(): Context?
    }

    interface IPresenter : IBasePresenter {
        fun checkServer(server: UWaziUploadServer)
    }
}