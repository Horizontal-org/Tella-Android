package org.horizontal.tella.mobile.domain.usecases.reports

import io.reactivex.Single
import org.horizontal.tella.mobile.domain.entity.reports.ReportInstanceBundle
import org.horizontal.tella.mobile.domain.repository.reports.ITellaReportsRepository
import org.horizontal.tella.mobile.domain.usecases.base.SingleUseCase
import org.horizontal.tella.mobile.views.fragment.reports.di.DefaultReports
import javax.inject.Inject

class GetReportBundleUseCase @Inject constructor(@DefaultReports private val reportsRepository: ITellaReportsRepository) :
    SingleUseCase<ReportInstanceBundle>() {

    private var id: Long = 0

    fun setId(id: Long) {
        this.id = id
    }

    override fun buildUseCaseSingle(): Single<ReportInstanceBundle> {
        return reportsRepository.getReportBundle(id)
    }
}