package rs.readahead.washington.mobile.domain.usecases.googledrive

import io.reactivex.Single
import rs.readahead.washington.mobile.domain.entity.reports.TellaReportServer
import rs.readahead.washington.mobile.domain.repository.ITellaUploadServersRepository
import rs.readahead.washington.mobile.domain.usecases.base.SingleUseCase
import rs.readahead.washington.mobile.views.fragment.googledrive.di.GoogleDrive
import javax.inject.Inject

class GetReportsServersUseCase @Inject constructor(@GoogleDrive private val serverRepository: ITellaUploadServersRepository) :
    SingleUseCase<List<TellaReportServer>>() {


    override fun buildUseCaseSingle(): Single<List<TellaReportServer>> {
        return serverRepository.listTellaUploadServers()
    }
}