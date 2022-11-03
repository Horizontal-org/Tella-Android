package rs.readahead.washington.mobile.domain.usecases.reports

import io.reactivex.Single
import rs.readahead.washington.mobile.domain.entity.reports.ReportFormInstance
import rs.readahead.washington.mobile.domain.repository.reports.ITellaReportsRepository
import rs.readahead.washington.mobile.domain.usecases.base.SingleUseCase
import javax.inject.Inject

class SaveReportFormInstanceUseCase @Inject constructor(private val reportsRepository: ITellaReportsRepository) :
    SingleUseCase<ReportFormInstance>() {

    private lateinit var reportFormInstance: ReportFormInstance

    fun setReportFormInstance(reportFormInstance: ReportFormInstance) {
        this.reportFormInstance = reportFormInstance
    }

    override fun buildUseCaseSingle(): Single<ReportFormInstance> {
        return reportsRepository.saveInstance(reportFormInstance)
    }
}