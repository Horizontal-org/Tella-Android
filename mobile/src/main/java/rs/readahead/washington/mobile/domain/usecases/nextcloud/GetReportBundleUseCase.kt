package rs.readahead.washington.mobile.domain.usecases.nextcloud

import io.reactivex.Single
import rs.readahead.washington.mobile.domain.entity.reports.ReportInstanceBundle
import rs.readahead.washington.mobile.domain.repository.reports.ITellaReportsRepository
import rs.readahead.washington.mobile.domain.usecases.base.SingleUseCase
import rs.readahead.washington.mobile.views.fragment.googledrive.di.GoogleDrive
import javax.inject.Inject

class GetReportBundleUseCase @Inject constructor(@GoogleDrive private val reportsRepository: ITellaReportsRepository) :
    SingleUseCase<ReportInstanceBundle>() {

    private var id: Long = 0

    fun setId(id: Long) {
        this.id = id
    }

    override fun buildUseCaseSingle(): Single<ReportInstanceBundle> {
        return reportsRepository.getReportBundle(id)
    }
}