package org.horizontal.tella.mobile.views.fragment.vault.home

import org.horizontal.tella.mobile.domain.entity.UWaziUploadServer
import org.horizontal.tella.mobile.domain.entity.collect.CollectServer
import org.horizontal.tella.mobile.domain.entity.dropbox.DropBoxServer
import org.horizontal.tella.mobile.domain.entity.googledrive.GoogleDriveServer
import org.horizontal.tella.mobile.domain.entity.nextcloud.NextCloudServer
import org.horizontal.tella.mobile.domain.entity.reports.TellaReportServer

data class ServerCounts(
    val dropBoxServers: List<DropBoxServer>,
    val googleDriveServers: List<GoogleDriveServer>,
    val tellaUploadServers: List<TellaReportServer>,
    val collectServers: List<CollectServer>,
    val uwaziServers: List<UWaziUploadServer>,
    val nextCloudServers: List<NextCloudServer>,
    )