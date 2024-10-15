package rs.readahead.washington.mobile.domain.usecases.dropbox

import io.reactivex.Completable
import rs.readahead.washington.mobile.domain.repository.reports.ITellaReportsRepository
import rs.readahead.washington.mobile.domain.usecases.base.CompletableUseCase
import rs.readahead.washington.mobile.views.fragment.dropbox.di.DropBox
import rs.readahead.washington.mobile.views.fragment.googledrive.di.GoogleDrive
import javax.inject.Inject

class DeleteReportUseCase @Inject constructor(@DropBox private val reportsRepository: ITellaReportsRepository) :
    CompletableUseCase() {

    private var id: Long = 0

    fun setId(id: Long) {
        this.id = id
    }

    override fun buildUseCaseSingle(): Completable {
        return reportsRepository.deleteReportInstance(id)
    }
}