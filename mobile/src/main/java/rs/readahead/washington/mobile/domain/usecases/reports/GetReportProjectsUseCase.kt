package rs.readahead.washington.mobile.domain.usecases.reports

import io.reactivex.Single
import rs.readahead.washington.mobile.domain.entity.reports.ProjectResult
import rs.readahead.washington.mobile.domain.entity.reports.TellaReportServer
import rs.readahead.washington.mobile.domain.repository.reports.ReportsRepository
import rs.readahead.washington.mobile.domain.usecases.base.SingleUseCase
import javax.inject.Inject

class GetReportProjectsUseCase @Inject internal constructor(val reportsRepository: ReportsRepository) :
    SingleUseCase<List<ProjectResult>>() {

    private lateinit var reportServerList: List<TellaReportServer>

    fun setReportServersList(reportServerList: List<TellaReportServer>) {
        this.reportServerList = reportServerList
    }

    override fun buildUseCaseSingle(): Single<List<ProjectResult>> {
        return reportsRepository.getProjects(limit = 100, offset = 100, servers = reportServerList)
    }
}