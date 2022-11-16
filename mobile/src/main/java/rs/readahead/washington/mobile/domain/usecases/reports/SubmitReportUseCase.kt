package rs.readahead.washington.mobile.domain.usecases.reports

import io.reactivex.Single
import rs.readahead.washington.mobile.data.entity.reports.ReportBodyEntity
import rs.readahead.washington.mobile.domain.entity.reports.ReportPostResult
import rs.readahead.washington.mobile.domain.entity.reports.TellaReportServer
import rs.readahead.washington.mobile.domain.repository.reports.ReportsRepository
import rs.readahead.washington.mobile.domain.usecases.base.SingleUseCase
import javax.inject.Inject

class SubmitReportUseCase @Inject constructor(val reportsRepository: ReportsRepository) :
    SingleUseCase<ReportPostResult>() {
    private lateinit var server: TellaReportServer
    private lateinit var reportBodyEntity: ReportBodyEntity

    fun setData(server: TellaReportServer, reportBodyEntity: ReportBodyEntity) {
        this.server = server
        this.reportBodyEntity = reportBodyEntity
    }

    override fun buildUseCaseSingle(): Single<ReportPostResult> {
        return reportsRepository.submitReport(server, reportBodyEntity)
    }
}