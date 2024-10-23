package rs.readahead.washington.mobile.domain.usecases.googledrive

import io.reactivex.Single
import rs.readahead.washington.mobile.domain.entity.googledrive.Config
import rs.readahead.washington.mobile.domain.entity.googledrive.GoogleDriveServer
import rs.readahead.washington.mobile.domain.repository.googledrive.ITellaGoogleDriveRepository
import rs.readahead.washington.mobile.domain.usecases.base.SingleUseCase
import javax.inject.Inject

class GetReportsServersUseCase @Inject constructor(
    private val serverRepository: ITellaGoogleDriveRepository,
    private val config: Config
) :
    SingleUseCase<List<GoogleDriveServer>>() {

    override fun buildUseCaseSingle(): Single<List<GoogleDriveServer>> {
        return serverRepository.listGoogleDriveServers(config.googleClientId)
    }
}