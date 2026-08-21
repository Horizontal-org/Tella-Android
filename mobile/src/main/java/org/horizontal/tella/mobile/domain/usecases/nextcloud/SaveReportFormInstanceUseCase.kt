package org.horizontal.tella.mobile.domain.usecases.nextcloud

import io.reactivex.Single
import org.horizontal.tella.mobile.domain.entity.reports.ReportInstance
import org.horizontal.tella.mobile.domain.repository.reports.ITellaReportsRepository
import org.horizontal.tella.mobile.domain.usecases.base.SingleUseCase
import org.horizontal.tella.mobile.views.fragment.nextCloud.di.NextCloud
import javax.inject.Inject

class SaveReportFormInstanceUseCase @Inject constructor(@NextCloud private val reportsRepository: ITellaReportsRepository) :
    SingleUseCase<ReportInstance>() {

    private lateinit var reportInstance: ReportInstance

    fun setReportFormInstance(reportInstance: ReportInstance) {
        this.reportInstance = reportInstance
    }

    override fun buildUseCaseSingle(): Single<ReportInstance> {
        return reportsRepository.saveInstance(reportInstance)
    }
}