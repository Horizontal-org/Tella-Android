package rs.readahead.washington.mobile.domain.usecases.nextcloud

import io.reactivex.Completable
import rs.readahead.washington.mobile.domain.repository.reports.ITellaReportsRepository
import rs.readahead.washington.mobile.domain.usecases.base.CompletableUseCase
import rs.readahead.washington.mobile.views.fragment.nextCloud.di.NextCloud
import javax.inject.Inject

class DeleteReportUseCase @Inject constructor(@NextCloud private val reportsRepository: ITellaReportsRepository) :
    CompletableUseCase() {

    private var id: Long = 0

    fun setId(id: Long) {
        this.id = id
    }

    override fun buildUseCaseSingle(): Completable {
        return reportsRepository.deleteReportInstance(id)
    }
}