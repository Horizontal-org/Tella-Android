package org.horizontal.tella.mobile.views.dialog

import org.horizontal.tella.mobile.bus.SingleLiveEvent
import org.horizontal.tella.mobile.domain.entity.UWaziUploadServer
import org.horizontal.tella.mobile.domain.entity.dropbox.DropBoxServer
import org.horizontal.tella.mobile.domain.entity.googledrive.GoogleDriveServer
import org.horizontal.tella.mobile.domain.entity.nextcloud.NextCloudServer
import org.horizontal.tella.mobile.domain.entity.reports.TellaReportServer

object SharedLiveData {
    val createServer = SingleLiveEvent<UWaziUploadServer>()
    val updateServer = SingleLiveEvent<UWaziUploadServer>()

    val createReportsServer = SingleLiveEvent<TellaReportServer>()
    val createReportsServerAndCloseActivity = SingleLiveEvent<TellaReportServer>()
    val updateReportsServer = SingleLiveEvent<TellaReportServer>()

    //Google Drive live data
    val createGoogleDriveServer = SingleLiveEvent<GoogleDriveServer>()
    val updateGoogleDriveServer = SingleLiveEvent<GoogleDriveServer>()

    //DropBox live data
    val createDropBoxServer = SingleLiveEvent<DropBoxServer>()
    val updateDropBoxServer = SingleLiveEvent<DropBoxServer>()

    //NextCloud server created
    val createNextCloudServer = SingleLiveEvent<NextCloudServer>()
}