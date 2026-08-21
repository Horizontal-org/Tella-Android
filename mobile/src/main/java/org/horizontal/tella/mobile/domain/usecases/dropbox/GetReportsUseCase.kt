package org.horizontal.tella.mobile.domain.usecases.dropbox

import io.reactivex.Single
import org.horizontal.tella.mobile.domain.entity.EntityStatus
import org.horizontal.tella.mobile.domain.entity.reports.ReportInstance
import org.horizontal.tella.mobile.domain.repository.reports.ITellaReportsRepository
import org.horizontal.tella.mobile.domain.usecases.base.SingleUseCase
import org.horizontal.tella.mobile.views.fragment.dropbox.di.DropBox
import javax.inject.Inject

class GetReportsUseCase @Inject constructor(@DropBox private val reportsRepository: ITellaReportsRepository) :
    SingleUseCase<List<ReportInstance>>() {

    private lateinit var entityStatus: EntityStatus

    fun setEntityStatus(entityStatus: EntityStatus) {
        this.entityStatus = entityStatus
    }

    override fun buildUseCaseSingle(): Single<List<ReportInstance>> {
        return when (entityStatus) {
            EntityStatus.SUBMITTED -> {
                reportsRepository.listSubmittedReportInstances()
            }

            EntityStatus.FINALIZED -> {
                reportsRepository.listOutboxReportInstances()
            }

            else -> {
                reportsRepository.listDraftReportInstances()
            }
        }
    }
}