package org.horizontal.tella.mobile.domain.usecases.reports

import io.reactivex.Single
import org.horizontal.tella.mobile.domain.entity.reports.TellaReportServer
import org.horizontal.tella.mobile.domain.repository.reports.ReportsRepository
import org.horizontal.tella.mobile.domain.usecases.base.SingleUseCase
import javax.inject.Inject

class CheckReportsServerUseCase @Inject constructor(private val reportsRepository: ReportsRepository) :
    SingleUseCase<TellaReportServer>() {

    private lateinit var server: TellaReportServer
    private lateinit var projectSlug: String

    fun saveServer(server: TellaReportServer, projectSlug: String) {
        this.server = server
        this.projectSlug = projectSlug
    }

    override fun buildUseCaseSingle(): Single<TellaReportServer> {
        return reportsRepository.login(server, projectSlug)
    }
}