package rs.readahead.washington.mobile.views.dialog

import rs.readahead.washington.mobile.bus.SingleLiveEvent
import rs.readahead.washington.mobile.domain.entity.UWaziUploadServer
import rs.readahead.washington.mobile.domain.entity.googledrive.GoogleDriveServer
import rs.readahead.washington.mobile.domain.entity.reports.TellaReportServer

object SharedLiveData {
    val createServer = SingleLiveEvent<UWaziUploadServer>()
    val updateServer = SingleLiveEvent<UWaziUploadServer>()

    val createReportsServer = SingleLiveEvent<TellaReportServer>()
    val createReportsServerAndCloseActivity = SingleLiveEvent<TellaReportServer>()
    val updateReportsServer = SingleLiveEvent<TellaReportServer>()

    //Google Drive live data
    val createGoogleDriveServer = SingleLiveEvent<GoogleDriveServer>()
    val updateGoogleDriveServer = SingleLiveEvent<GoogleDriveServer>()
}