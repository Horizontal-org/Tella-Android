package rs.readahead.washington.mobile.domain.usecases.googledrive

import io.reactivex.Single
import rs.readahead.washington.mobile.domain.entity.googledrive.GoogleDriveServer
import rs.readahead.washington.mobile.domain.entity.reports.TellaReportServer
import rs.readahead.washington.mobile.domain.repository.ITellaUploadServersRepository
import rs.readahead.washington.mobile.domain.repository.googledrive.IGoogleDriveRepository
import rs.readahead.washington.mobile.domain.usecases.base.SingleUseCase
import rs.readahead.washington.mobile.views.fragment.googledrive.di.GoogleDrive
import javax.inject.Inject

class GetReportsServersUseCase @Inject constructor(private val serverRepository: IGoogleDriveRepository) :
    SingleUseCase<List<GoogleDriveServer>>() {


    override fun buildUseCaseSingle(): Single<List<GoogleDriveServer>> {
        return serverRepository.listGoogleDriveServers()
    }
}