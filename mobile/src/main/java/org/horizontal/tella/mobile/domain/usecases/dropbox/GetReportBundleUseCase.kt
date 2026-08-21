package org.horizontal.tella.mobile.domain.usecases.dropbox

import io.reactivex.Single
import org.horizontal.tella.mobile.domain.entity.reports.ReportInstanceBundle
import org.horizontal.tella.mobile.domain.repository.reports.ITellaReportsRepository
import org.horizontal.tella.mobile.domain.usecases.base.SingleUseCase
import org.horizontal.tella.mobile.views.fragment.dropbox.di.DropBox
import javax.inject.Inject

class GetReportBundleUseCase @Inject constructor(@DropBox private val reportsRepository: ITellaReportsRepository) :
    SingleUseCase<ReportInstanceBundle>() {

    private var id: Long = 0

    fun setId(id: Long) {
        this.id = id
    }

    override fun buildUseCaseSingle(): Single<ReportInstanceBundle> {
        return reportsRepository.getReportBundle(id)
    }
}