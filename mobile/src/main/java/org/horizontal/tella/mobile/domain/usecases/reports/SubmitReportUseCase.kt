package org.horizontal.tella.mobile.domain.usecases.reports

import io.reactivex.Single
import org.horizontal.tella.mobile.data.entity.reports.ReportBodyEntity
import org.horizontal.tella.mobile.domain.entity.reports.ReportPostResult
import org.horizontal.tella.mobile.domain.entity.reports.TellaReportServer
import org.horizontal.tella.mobile.domain.repository.reports.ReportsRepository
import org.horizontal.tella.mobile.views.fragment.reports.di.DefaultReports
import javax.inject.Inject

class SubmitReportUseCase @Inject constructor(@DefaultReports val reportsRepository: ReportsRepository) {
    private lateinit var server: TellaReportServer
    private lateinit var reportBodyEntity: ReportBodyEntity

    fun setData(server: TellaReportServer, reportBodyEntity: ReportBodyEntity) {
        this.server = server
        this.reportBodyEntity = reportBodyEntity
    }

    fun buildUseCaseSingle(): Single<ReportPostResult> {
        return reportsRepository.submitReport(server, reportBodyEntity)
    }
}