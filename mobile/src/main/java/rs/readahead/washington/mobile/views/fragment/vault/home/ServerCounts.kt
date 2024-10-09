package rs.readahead.washington.mobile.views.fragment.vault.home

import rs.readahead.washington.mobile.domain.entity.UWaziUploadServer
import rs.readahead.washington.mobile.domain.entity.collect.CollectServer
import rs.readahead.washington.mobile.domain.entity.googledrive.GoogleDriveServer
import rs.readahead.washington.mobile.domain.entity.reports.TellaReportServer

data class ServerCounts(
    val googleDriveServers: List<GoogleDriveServer>,
    val tellaUploadServers: List<TellaReportServer>,
    val collectServers: List<CollectServer>,
    val uwaziServers: List<UWaziUploadServer>
)