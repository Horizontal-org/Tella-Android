package org.horizontal.tella.mobile.mvp.contract

import android.content.Context
import org.horizontal.tella.mobile.domain.entity.reports.TellaReportServer
import org.horizontal.tella.mobile.domain.entity.UploadProgressInfo

class ICheckUwaziServer {
    interface IView {
        fun onServerCheckSuccess()
        fun onServerCheckFailure(status: UploadProgressInfo.Status?)
        fun onServerCheckError(error: Throwable?)
        fun showServerCheckLoading()
        fun hideServerCheckLoading()
        fun onNoConnectionAvailable()
        fun setSaveAnyway(enabled: Boolean)
        val context: Context?
    }

    interface IPresenter : IBasePresenter {
        fun checkServer(server: TellaReportServer?, connectionRequired: Boolean)
    }
}