package org.horizontal.tella.mobile.domain.usecases.reports

import io.reactivex.Completable
import org.horizontal.tella.mobile.domain.repository.reports.ITellaReportsRepository
import org.horizontal.tella.mobile.domain.usecases.base.CompletableUseCase
import org.horizontal.tella.mobile.views.fragment.reports.di.DefaultReports
import javax.inject.Inject

class DeleteReportUseCase @Inject constructor(@DefaultReports private val reportsRepository: ITellaReportsRepository) :
    CompletableUseCase() {

    private var id: Long = 0

    fun setId(id: Long) {
        this.id = id
    }

    override fun buildUseCaseSingle(): Completable {
        return reportsRepository.deleteReportInstance(id)
    }
}