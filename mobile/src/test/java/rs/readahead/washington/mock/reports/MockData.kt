package rs.readahead.washington.mock.reports

import rs.readahead.washington.mobile.domain.entity.reports.TellaReportServer

object MockData {

    val tellaReportServer = TellaReportServer(id = 2).apply {
        name = "Tella report server"
        username = "tellaUser"
        password = "tellauser123"
        url = "http://www:test.com"
    }
}