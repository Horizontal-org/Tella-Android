package rs.readahead.washington.mobile.domain.repository.reports

import io.reactivex.Single
import rs.readahead.washington.mobile.data.entity.reports.ReportBodyEntity
import rs.readahead.washington.mobile.domain.entity.reports.ReportPostResult
import rs.readahead.washington.mobile.domain.entity.reports.ReportsLoginResult
import rs.readahead.washington.mobile.domain.entity.reports.TellaReportServer

interface ReportsRepository {
    fun login(server: TellaReportServer): Single<ReportsLoginResult>
    fun submitReport(server: TellaReportServer, reportBody: ReportBodyEntity): Single<ReportPostResult>
}